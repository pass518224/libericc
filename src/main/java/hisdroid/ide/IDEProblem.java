package hisdroid.ide;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import heros.DefaultSeeds;
import heros.EdgeFunction;
import heros.EdgeFunctions;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import heros.flowfunc.Transfer;
import hisdroid.ValueType;
import hisdroid.callhandler.CallHandler;
import hisdroid.callhandler.Handlers;
import hisdroid.edgefunc.*;
import hisdroid.flowfunc.KillAllExceptStaticField;
import hisdroid.value.*;
import soot.NullType;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;

public class IDEProblem extends DefaultJimpleIDETabulationProblem<Value, GeneralValue, InterproceduralCFG<Unit,SootMethod>> {
	static final Logger logger = Logger.getLogger("HisDroid");
	static PointsToAnalysis pointsToAnalysis = Scene.v().getPointsToAnalysis();
	
	SootMethod mainMethod = null;
	final InterproceduralCFG<Unit,SootMethod> icfg;
	IDEUtility utility = new IDEUtility(this);
	
	public IDEProblem(InterproceduralCFG<Unit,SootMethod> icfg) {
		this(icfg, Scene.v().getMainMethod());
	}
	
	public IDEProblem(InterproceduralCFG<Unit,SootMethod> icfg, SootMethod mainMethod) {
		super(icfg);
		this.mainMethod = mainMethod;
		this.icfg = icfg;
	}

	@Override
	protected EdgeFunction<GeneralValue> createAllTopFunction(){
		return new AllTop<GeneralValue>(TopValue.v());
	}

	@Override
	protected JoinLattice<GeneralValue> createJoinLattice(){
		return new hisdroid.lattice.GeneralLattice();
	}	

	@Override
	protected EdgeFunctions<Unit, Value, SootMethod, GeneralValue> createEdgeFunctionsFactory(){
		return new EdgeFunctions<Unit, Value, SootMethod, GeneralValue>() {
			
			@Override
			public EdgeFunction<GeneralValue> getNormalEdgeFunction(Unit curr, Value currNode, Unit succ, Value succNode){
				EdgeFunction<GeneralValue> ret = getNormalEdge(curr, currNode, succ, succNode);
				logger.finest(String.format("%s: %s -> %s [%s]", curr, currNode, succNode, ret));
				return ret;
			}

			@Override
			public EdgeFunction<GeneralValue> getCallEdgeFunction(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode) {
				EdgeFunction<GeneralValue> ret = getCallEdge(callStmt, srcNode, destinationMethod, destNode);
				logger.finest(String.format("(Call) %s, %s: %s -> %s [%s]", callStmt, destinationMethod, srcNode, destNode, ret));
				return ret;
			}

			@Override
			public EdgeFunction<GeneralValue> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Value exitNode, Unit returnSite, Value retNode){
				EdgeFunction<GeneralValue> ret = getReturnEdge(callSite, calleeMethod, exitStmt, exitNode, returnSite, retNode);
				logger.finest(String.format("(Ret) %s, %s, %s: %s -> %s [%s]", callSite, calleeMethod, exitStmt, exitNode, retNode, ret));
				return ret;
			}

			@Override
			public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode){
				EdgeFunction<GeneralValue> ret = getCallToReturnEdge(callSite, callNode, returnSite, returnSideNode);
				logger.finest(String.format("(C2R) %s: %s -> %s [%s]", callSite, callNode, returnSideNode, ret));
				return ret;
			}
		};
	}
	

	@Override
	protected FlowFunctions<Unit, Value, SootMethod> createFlowFunctionsFactory(){
		return new FlowFunctions<Unit, Value, SootMethod>(){

			@Override
			public FlowFunction<Value> getNormalFlowFunction(Unit curr, Unit succ){
				return getNormalFlow(curr, succ);
			}

			@Override
			public FlowFunction<Value> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
				return getCallFlow(callStmt, destinationMethod);
			}

			@Override
			public FlowFunction<Value> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite){
				return getReturnFlow(callSite, calleeMethod, exitStmt, returnSite);
			}

			@Override
			public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite){
				return getCallToReturnFlow(callSite, returnSite);
			}
		};
	}
	
	@Override
	public Map<Unit, Set<Value>> initialSeeds(){
		return DefaultSeeds.make(Collections.singleton(mainMethod.getActiveBody().getUnits().getFirst()), zeroValue());
	}

	@Override
	protected Value createZeroValue(){
		return new JimpleLocal("<<zero>>", NullType.v());
	}
	
	EdgeFunction<GeneralValue> getNormalEdge(Unit curr, Value currNode, Unit succ, Value succNode){
		if (curr instanceof AssignStmt) {
			final AssignStmt assign = (AssignStmt) curr;
			final Value leftOp = assign.getLeftOp();
			final Value rightOp = assign.getRightOp();
			
			if (rightOp instanceof Constant) {
				if (currNode.equivTo(zeroValue()) && succNode.equivTo(leftOp)) {
					return utility.getEdgeFromConstant((Constant)rightOp);
				}
			}
			if (rightOp instanceof CastExpr) {
				Value r = ((CastExpr)rightOp).getOp();
				if (currNode.equivTo(r) && succNode.equivTo(leftOp)) {
					Type t = ((CastExpr)rightOp).getCastType();
					if (t instanceof IntType) {
						return new CastEdge(ValueType.Int);
					}
					if (t instanceof LongType) {
						return new CastEdge(ValueType.Long);
					}
					if (t instanceof FloatType) {
						return new CastEdge(ValueType.Float);
					}
					if (t instanceof DoubleType) {
						return new CastEdge(ValueType.Double);
					}
				}
				return allTopFunction();
			}
			if (rightOp instanceof CmpExpr || rightOp instanceof CmplExpr || rightOp instanceof CmpgExpr) {
				BinopExpr be = (BinopExpr) rightOp;
				Value op1 = be.getOp1(), op2 = be.getOp2();
				return new ConstantEdge(new CmpValue(curr, op1, op2));
			}
		}
		return EdgeIdentity.v();
	}
	
	EdgeFunction<GeneralValue> getCallEdge(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode){
		Stmt stmt = (Stmt) callStmt;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getCallEdgeFunction(callStmt, srcNode, destinationMethod, destNode, zeroValue());
		}
		
		if (srcNode.equivTo(zeroValue()) && !destNode.equivTo(zeroValue())) {
			int ind = destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode);
			Value arg = ie.getArgs().get(ind);
			if (arg instanceof Constant) {
				return utility.getEdgeFromConstant((Constant)arg);
			}
		}
		return EdgeIdentity.v();
	}
	
	EdgeFunction<GeneralValue> getReturnEdge(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Value exitNode, Unit returnSite, Value retNode){
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getReturnEdgeFunction(callSite, calleeMethod, exitStmt, exitNode, returnSite, retNode, zeroValue());
		}
		if (exitNode.equivTo(zeroValue()) && !retNode.equivTo(zeroValue())) {
			ReturnStmt returnStmt = (ReturnStmt) exitStmt;
			Value v = returnStmt.getOp();
			if (v instanceof Constant) {
				return utility.getEdgeFromConstant((Constant)v);
			}
		}
		return EdgeIdentity.v();
	}
	
	EdgeFunction<GeneralValue> getCallToReturnEdge(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode){
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();

		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getCallToReturnEdgeFunction(callSite, callNode, returnSite, returnSideNode, zeroValue());
		}
		return EdgeIdentity.v();
	}
	
	
	
	FlowFunction<Value> getNormalFlow(Unit curr, Unit succ){
		if (curr instanceof AssignStmt) {
			final AssignStmt assign = (AssignStmt) curr;
			final Value leftOp = assign.getLeftOp();
			final Value rightOp = assign.getRightOp();
			if (rightOp instanceof Constant) {
				return new Transfer<Value>(leftOp, zeroValue());
			}
			if (rightOp instanceof CastExpr) {
				Value r = ((CastExpr)rightOp).getOp();
				return new Transfer<Value>(leftOp, r);
			}
			if (rightOp instanceof CmpExpr || rightOp instanceof CmplExpr || rightOp instanceof CmpgExpr) {
				return new Transfer<Value>(leftOp, zeroValue());
			}
			return new FlowFunction<Value>(){
				@Override
				public Set<Value> computeTargets(Value source){
					Set<Value> res = new HashSet<Value>();
					if (source.equivTo(rightOp)) {
						res.add(leftOp);
					}
					else {
						PointsToSet rightPointsToSet = null, sourcePointsToSet = null;
						if (rightOp instanceof Local) {
							Local rightLocal = (Local) rightOp;
							rightPointsToSet = pointsToAnalysis.reachingObjects(rightLocal);
						}
						else if (rightOp instanceof StaticFieldRef){
							StaticFieldRef rightStaticFieldRef = (StaticFieldRef) rightOp;
							rightPointsToSet = pointsToAnalysis.reachingObjects(rightStaticFieldRef.getField());
						}
						else if (rightOp instanceof InstanceFieldRef){
							InstanceFieldRef rightInstanceFieldRef = (InstanceFieldRef) rightOp;
							PointsToSet set = pointsToAnalysis.reachingObjects((Local) rightInstanceFieldRef.getBase());
							rightPointsToSet = pointsToAnalysis.reachingObjects(set, rightInstanceFieldRef.getField());
						}
						if (source instanceof Local) {
							Local sourceLocal = (Local) source;
							sourcePointsToSet = pointsToAnalysis.reachingObjects(sourceLocal);
						}
						else if (source instanceof StaticFieldRef){
							StaticFieldRef sourceStaticFieldRef = (StaticFieldRef) source;
							rightPointsToSet = pointsToAnalysis.reachingObjects(sourceStaticFieldRef.getField());
						}
						else if (source instanceof InstanceFieldRef){
							InstanceFieldRef sourceInstanceFieldRef = (InstanceFieldRef) source;
							PointsToSet set = pointsToAnalysis.reachingObjects((Local) sourceInstanceFieldRef.getBase());
							sourcePointsToSet = pointsToAnalysis.reachingObjects(set, sourceInstanceFieldRef.getField());
						}
						if (rightPointsToSet != null && sourcePointsToSet != null && rightPointsToSet.hasNonEmptyIntersection(sourcePointsToSet)) {
							res.add(leftOp);
						}
					}
					
					if (!source.equivTo(leftOp)) {
						res.add(source);
					}
					return res;
				}
			};
			
		}
		return Identity.v();
	}

	FlowFunction<Value> getCallFlow(Unit callStmt, SootMethod destinationMethod) {
		if ("<clinit>".equals(destinationMethod.getName())) {
			return KillAllExceptStaticField.v();
		}
		
		Stmt stmt = (Stmt) callStmt;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getCallFlowFunction(callStmt, destinationMethod, zeroValue());
		}
		
		final List<Value> callArgs = ie.getArgs();
		final List<Value> paramLocals = new ArrayList<Value>();
		for(int i=0;i<destinationMethod.getParameterCount();i++) {
			paramLocals.add(destinationMethod.getActiveBody().getParameterLocal(i));
		}
		//assert callArgs.size() == paramLocals.size() : callStmt + " " + destinationMethod;
		
		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				Set<Value> ret = new HashSet<Value>();
				if (source.equivTo(zeroValue())) {
					for (int i=0; i<callArgs.size(); i++) {
						if (callArgs.get(i) instanceof Constant && i < paramLocals.size()) {
							ret.add(paramLocals.get(i));
						}
					}
				}
				else {
					if (source instanceof StaticFieldRef) {
						ret.add(source);
					}
					for (int i=0; i<callArgs.size(); i++) {
						if (callArgs.get(i).equivTo(source) && i < paramLocals.size()) {
							ret.add(paramLocals.get(i));
						}
					}
				}
				return ret;
			}
		};
	}

	FlowFunction<Value> getReturnFlow(final Unit callSite, final SootMethod calleeMethod, Unit exitStmt, Unit returnSite){
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getReturnFlowFunction(callSite, calleeMethod, exitStmt, returnSite, zeroValue());
		}
		
		if (exitStmt instanceof ReturnStmt) {								
			ReturnStmt returnStmt = (ReturnStmt) exitStmt;
			final Value retOp = returnStmt.getOp();
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					Set<Value> ret = new HashSet<Value>();
					if (source instanceof StaticFieldRef) {
						ret.add(source);
					}
					if (callSite instanceof DefinitionStmt) {
						DefinitionStmt defnStmt = (DefinitionStmt) callSite;
						final Value leftOp = defnStmt.getLeftOp();
						if (retOp instanceof Constant) {
							if (source.equivTo(zeroValue())) {
								ret.add(leftOp);
							}
						}
						else if (source.equivTo(retOp)) {
							ret.add(leftOp);
						}
						
					}
					return ret;
				}
			};
		}
		if (exitStmt instanceof ReturnVoidStmt) {
			return KillAllExceptStaticField.v();
		} 
		return KillAll.v();
	}

	FlowFunction<Value> getCallToReturnFlow(final Unit callSite, Unit returnSite){
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		CallHandler ch = Handlers.getHandler(ie.getMethod());
		if (ch != null) {
			return ch.getCallToReturnFlowFunction(callSite, returnSite, zeroValue());
		}
		
		if (icfg.getCalleesOfCallAt(callSite).isEmpty()) {
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					if (callSite instanceof DefinitionStmt) {
						DefinitionStmt definition = (DefinitionStmt) callSite;
						if (source.equivTo(definition.getLeftOp())) {
							return Collections.emptySet();
						}
					}
					return Collections.singleton(source);
				}
			};
		}
		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				if (source instanceof StaticFieldRef){
					return Collections.emptySet();
				}
				if (callSite instanceof DefinitionStmt) {
					DefinitionStmt definition = (DefinitionStmt) callSite;
					if (source.equivTo(definition.getLeftOp())) {
						return Collections.emptySet();
					}
				}
				return Collections.singleton(source);
			}
		};
	}

}
