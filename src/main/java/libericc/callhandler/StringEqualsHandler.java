package libericc.callhandler;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import libericc.edgefunc.ConstantEdge;
import libericc.flowfunc.Transfer;
import libericc.value.EqualityValue;
import libericc.value.GeneralValue;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;

public class StringEqualsHandler extends CallHandler {
	@Override
	public Set<MethodSig> getTargets(){
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("java.lang.String", "boolean equals(java.lang.Object)"));
		return targets;
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, final Value zeroValue){
		if (callSite instanceof AssignStmt) {
			AssignStmt stmt = (AssignStmt) callSite;
			final Value leftOp = stmt.getLeftOp();
			return new Transfer<Value>(leftOp, zeroValue);
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
			final Value arg0 = iie.getArg(0);
	
			if (callNode == zeroValue && returnSideNode.equivTo(leftOp)) {
				return new ConstantEdge(new EqualityValue(stmt, base, arg0));
			}
		}
		return EdgeIdentity.v();
	}
}
