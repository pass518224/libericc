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
import hisdroid.callhandler.CallHandler;
import hisdroid.callhandler.Handlers;
import hisdroid.edgefunc.*;
import hisdroid.value.*;
import soot.NullType;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.Local;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;

public class IDEProblem extends DefaultJimpleIDETabulationProblem<Value, GeneralValue, InterproceduralCFG<Unit,SootMethod>> {
	static final Logger logger = Logger.getLogger("IDELog");
	static PointsToAnalysis pointsToAnalysis = Scene.v().getPointsToAnalysis();
	
	SootMethod mainMethod = null;
	final InterproceduralCFG<Unit,SootMethod> icfg;
	
	public IDEProblem(InterproceduralCFG<Unit,SootMethod> icfg) {
		this(icfg, Scene.v().getMainMethod());
	}
	
	public IDEProblem(InterproceduralCFG<Unit,SootMethod> icfg, SootMethod mainMethod) {
		super(icfg);
		logger.info("Start IDE analysis");
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
				if (curr instanceof AssignStmt) {
					final AssignStmt assign = (AssignStmt) curr;
					final Value leftOp = assign.getLeftOp();
					final Value rightOp = assign.getRightOp();
					if (leftOp.equivTo(succNode)) {
						if (currNode.equivTo(zeroValue())) {
							if (rightOp instanceof IntConstant) {
								final IntConstant rightOpConst = (IntConstant) rightOp;
								int i = rightOpConst.value;
								logger.finest(String.format("%s: %s -> %s [ConstantEdge(%s)]", curr, currNode, succNode, i));
								return new ConstantEdge(new IntValueSet(i));
							}
							if (rightOp instanceof StringConstant) {
								final StringConstant rightOpConst = (StringConstant) rightOp;
								String s = rightOpConst.value;
								logger.finest(String.format("%s: %s -> %s [ConstantEdge(\"%s\")]", curr, currNode, succNode, s));
								return new ConstantEdge(new StringValueSet(s));
							}
						}
					}
				}
				logger.finest(String.format("%s: %s -> %s [id]", curr, currNode, succNode));
				return EdgeIdentity.v();
			}

			@Override
			public EdgeFunction<GeneralValue> getCallEdgeFunction(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode) {
				Stmt stmt = (Stmt) callStmt;
				InvokeExpr ie = stmt.getInvokeExpr();
				
				CallHandler ch = Handlers.getHandler(ie.getMethod());
				if (ch != null) {
					return ch.getCallEdgeFunction(callStmt, srcNode, destinationMethod, destNode, zeroValue());
				}
				
				if (srcNode.equivTo(zeroValue()) && !destNode.equivTo(zeroValue())) {
					int ind = destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode);
					IntConstant c = (IntConstant) stmt.getInvokeExpr().getArgs().get(ind);
					int i = c.value;
					logger.finest(String.format("(Call) %s, %s: %s -> %s [ConstantEdge(%s)]", callStmt, destinationMethod, srcNode, destNode, i));
					return new ConstantEdge(new IntValueSet(i));
				}
				logger.finest(String.format("(Call) %s, %s: %s -> %s [id]", callStmt, destinationMethod, srcNode, destNode));
				return EdgeIdentity.v();
			}

			@Override
			public EdgeFunction<GeneralValue> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Value exitNode, Unit returnSite, Value retNode){
				Stmt stmt = (Stmt) callSite;
				InvokeExpr ie = stmt.getInvokeExpr();
				
				CallHandler ch = Handlers.getHandler(ie.getMethod());
				if (ch != null) {
					return ch.getReturnEdgeFunction(callSite, calleeMethod, exitStmt, exitNode, returnSite, retNode, zeroValue());
				}
				if (exitNode.equivTo(zeroValue()) && !retNode.equivTo(zeroValue())) {
					ReturnStmt returnStmt = (ReturnStmt) exitStmt;
					Value op = returnStmt.getOp();
					IntConstant c = (IntConstant) op;
					int i = c.value;
					logger.finest(String.format("(Ret) %s, %s, %s: %s -> %s [ConstantEdge(%s)]", callSite, calleeMethod, exitStmt, exitNode, retNode, i));
					return new ConstantEdge(new IntValueSet(i));
				}
				logger.finest(String.format("(Ret) %s, %s, %s: %s -> %s [id]", callSite, calleeMethod, exitStmt, exitNode, retNode));
				return EdgeIdentity.v();
			}

			@Override
			public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode){
				Stmt stmt = (Stmt) callSite;
				InvokeExpr ie = stmt.getInvokeExpr();
				
				
				CallHandler ch = Handlers.getHandler(ie.getMethod());
				if (ch != null) {
					return ch.getCallToReturnEdgeFunction(callSite, callNode, returnSite, returnSideNode, zeroValue());
				}
				return EdgeIdentity.v();
			}
		};
	}
	

	@Override
	protected FlowFunctions<Unit, Value, SootMethod> createFlowFunctionsFactory(){
		return new FlowFunctions<Unit, Value, SootMethod>(){

			@Override
			public FlowFunction<Value> getNormalFlowFunction(Unit curr, Unit succ){
				if (curr instanceof AssignStmt) {
					final AssignStmt assign = (AssignStmt) curr;
					final Value leftOp = assign.getLeftOp();
					final Value rightOp = assign.getRightOp();
					if (rightOp instanceof IntConstant) {
						return new Transfer<Value>(leftOp, zeroValue());
					}
					if (rightOp instanceof StringConstant) {
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

			@Override
			public FlowFunction<Value> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
				if ("<clinit>".equals(destinationMethod.getName())) {
					return KillAll.v();
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
				
				return new FlowFunction<Value>() {
					@Override
					public Set<Value> computeTargets(Value source) {
						Set<Value> ret = new HashSet<Value>();
						if (source.equivTo(zeroValue())) {
							for (int i=0; i<callArgs.size(); i++) {
								if (callArgs.get(i) instanceof IntConstant) {
									ret.add(paramLocals.get(i));
								}
							}
						}
						else {
							if (source instanceof StaticFieldRef) {
								ret.add(source);
							}
							for (int i=0; i<callArgs.size(); i++) {
								if (callArgs.get(i).equivTo(source)) {
									ret.add(paramLocals.get(i));
								}
							}
						}
						return ret;
					}
				};
			}

			@Override
			public FlowFunction<Value> getReturnFlowFunction(final Unit callSite, final SootMethod calleeMethod, Unit exitStmt, Unit returnSite){
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
								if (retOp instanceof IntConstant) {
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
					return new FlowFunction<Value>() {
						@Override
						public Set<Value> computeTargets(Value source) {
							if (source instanceof StaticFieldRef) {
								return Collections.singleton(source);
							}
							return Collections.emptySet();
						}
					};
				} 
				return KillAll.v();
			}

			@Override
			public FlowFunction<Value> getCallToReturnFlowFunction(final Unit callSite, Unit returnSite){
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
}
