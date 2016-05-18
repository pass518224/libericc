package hisdroid.callhandler;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import heros.flowfunc.Transfer;
import hisdroid.edgefunc.StringHashCodeEdge;
import hisdroid.value.GeneralValue;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;

public class StringHashCodeHandler extends CallHandler {
	@Override
	public Set<MethodSig> getTargets(){
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("java.lang.String", "int hashCode()"));
		return targets;
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, final Value zeroValue){
		if (callSite instanceof AssignStmt) {
			AssignStmt stmt = (AssignStmt) callSite;
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();
			final Value leftOp = stmt.getLeftOp();
			final Value base = iie.getBase();
			return new Transfer<Value>(leftOp, base);
		}
		return Identity.v();
	}
	
	@Override
	public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode, Value zeroValue){
		if (callSite instanceof AssignStmt) {
			AssignStmt stmt = (AssignStmt) callSite;
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();
			final Value leftOp = stmt.getLeftOp();
			final Value base = iie.getBase();
	
			if (callNode.equivTo(base) && returnSideNode.equivTo(leftOp)) {
				return new StringHashCodeEdge();
			}
		}
		return EdgeIdentity.v();
	}
}
