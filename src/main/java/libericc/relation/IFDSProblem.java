package libericc.relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import libericc.Logger;
import soot.NullType;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;

public class IFDSProblem extends DefaultJimpleIFDSTabulationProblem<Value, InterproceduralCFG<Unit,SootMethod>> {
	InterproceduralCFG<Unit,SootMethod> icfg;

	public IFDSProblem(InterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
		this.icfg = icfg;
	}

	@Override
	public Map<Unit, Set<Value>> initialSeeds() {
		return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()), zeroValue());
	}

	@Override
	protected FlowFunctions<Unit, Value, SootMethod> createFlowFunctionsFactory() {
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
	protected Value createZeroValue() {
		return new JimpleLocal("<<zero>>", NullType.v());
	}
	

	FlowFunction<Value> getNormalFlow(Unit curr, Unit succ){
		if (curr instanceof AssignStmt) {
			final AssignStmt assign = (AssignStmt) curr;
			final Value leftOp = assign.getLeftOp();
			final Value rightOp = assign.getRightOp();
			
			return new FlowFunction<Value>(){
				@Override
				public Set<Value> computeTargets(Value source){
					if (source.equivTo(leftOp)) {
						return Collections.emptySet();
					}
					
					Set<Value> res = new HashSet<Value>();
					res.add(source);
					
					if (source.equivTo(rightOp)) {
						res.add(leftOp);
					}
					if (rightOp instanceof InstanceFieldRef) {
						Value base = ((InstanceFieldRef)rightOp).getBase();
						if (source.equivTo(base)) res.add(leftOp);
					}
					if (rightOp instanceof ArrayRef) {
						Value base = ((ArrayRef)rightOp).getBase();
						if (source.equivTo(base)) res.add(leftOp);
					}
					if (rightOp instanceof CastExpr) {
						if (source.equivTo(((CastExpr)rightOp).getOp())) res.add(leftOp);
					}
					return res;
				}
			};
			
		}
		return Identity.v();
	}

	FlowFunction<Value> getCallFlow(Unit callStmt, final SootMethod destinationMethod) {
		if ("<clinit>".equals(destinationMethod.getName())) {
			return KillAll.v();
		}

		Stmt stmt = (Stmt) callStmt;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		final List<Value> callArgs = ie.getArgs();
		final List<Value> paramLocals = new ArrayList<Value>();
		for (int i=0;i<destinationMethod.getParameterCount();i++) {
			paramLocals.add(destinationMethod.getActiveBody().getParameterLocal(i));
		}
		
		final boolean isIccMethod = IFDSDummyMainCreator.iccMethods.contains(destinationMethod);
		
		Value base = null;
		if (ie instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;
			base = vie.getBase();
		}
		else if (ie instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr iie = (InterfaceInvokeExpr) ie;
			base = iie.getBase();
		}
		else if (ie instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr iie = (SpecialInvokeExpr) ie;
			base = iie.getBase();
		}
		final Value baseF = base;

		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				Set<Value> ret = new HashSet<Value>();
				if (source instanceof StaticFieldRef) {
					ret.add(source);
				}
				for (int i=0; i<callArgs.size(); i++) {
					if (callArgs.get(i).equivTo(source) && i < paramLocals.size()) {
						ret.add(paramLocals.get(i));
					}
				}
				if (source.equals(zeroValue()) && isIccMethod) {
					for (int i=0; i<callArgs.size(); i++) {
						ret.add(paramLocals.get(i));
					}
				}
				if (baseF != null && source.equals(baseF) && !isIccMethod) {
					ret.add(destinationMethod.retrieveActiveBody().getThisLocal());
				}
				return ret;
			}
		};
	}

	FlowFunction<Value> getReturnFlow(final Unit callSite, final SootMethod calleeMethod, Unit exitStmt, Unit returnSite){
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();
		
		if (IFDSDummyMainCreator.iccMethods.contains(calleeMethod)) {
			return KillAll.v();
		}

		Value base = null;
		if (ie instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;
			base = vie.getBase();
		}
		else if (ie instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr iie = (InterfaceInvokeExpr) ie;
			base = iie.getBase();
		}
		else if (ie instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr iie = (SpecialInvokeExpr) ie;
			base = iie.getBase();
		}
		final Value baseF = base;

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
					if (callSite instanceof DefinitionStmt && source.equivTo(retOp)) {
						DefinitionStmt defnStmt = (DefinitionStmt) callSite;
						ret.add(defnStmt.getLeftOp());
					}
					if (baseF != null && source.equals(calleeMethod.retrieveActiveBody().getThisLocal())) {
						ret.add(baseF);
					}
					return ret;
				}
			};
		}
		if (exitStmt instanceof ReturnVoidStmt) {
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					Set<Value> ret = new HashSet<Value>();
					if (source instanceof StaticFieldRef) {
						ret.add(source);
					}
					if (baseF != null && source.equals(calleeMethod.retrieveActiveBody().getThisLocal())) {
						ret.add(baseF);
					}
					return ret;
				}
			};
		} 
		return KillAll.v();
	}

	FlowFunction<Value> getCallToReturnFlow(final Unit callSite, Unit returnSite){
		Logger.getLogger().finest("C2R flow at "+callSite);
		Stmt stmt = (Stmt) callSite;
		InvokeExpr ie = stmt.getInvokeExpr();
		final List<Value> callArgs = ie.getArgs();

		Value base = null;
		Value leftOp = null;

		if (ie instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;
			base = vie.getBase();
		}
		else if (ie instanceof InterfaceInvokeExpr) {
			InterfaceInvokeExpr iie = (InterfaceInvokeExpr) ie;
			base = iie.getBase();
		}
		else if (ie instanceof SpecialInvokeExpr) {
			SpecialInvokeExpr iie = (SpecialInvokeExpr) ie;
			base = iie.getBase();
		}
		
		if (callSite instanceof DefinitionStmt) {
			DefinitionStmt defnStmt = (DefinitionStmt) callSite;
			leftOp = defnStmt.getLeftOp();
		}

		final Value baseF = base;
		final Value leftOpF = leftOp;
		
		// use assumption if no callees to analyze
		if (icfg.getCalleesOfCallAt(callSite).isEmpty()) {
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					Set<Value> ret = new HashSet<Value>();
					ret.add(source);
					// taint leftOp if base is tainted
					if (baseF != null && leftOpF != null && source.equivTo(baseF)) {
						ret.add(leftOpF);
					}
					// taint leftOp if one of the args is tainted
					if (leftOpF != null && callArgs.contains(source)) {
						ret.add(leftOpF);
					}
					// taint base if one of the args is tainted and has no callee in known methods
					if (baseF != null && callArgs.contains(source)) {
						ret.add(baseF);
					}
					return ret;
				}
			};
		}
		return Identity.v();
	}
}
