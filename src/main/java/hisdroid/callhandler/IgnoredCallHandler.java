package hisdroid.callhandler;

import java.util.Collections;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import heros.flowfunc.Transfer;
import hisdroid.edgefunc.ConstantEdge;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;

public class IgnoredCallHandler extends CallHandler {

	@Override
	public Set<MethodSig> getTargets() {
		return Collections.emptySet();
	}

	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, Value zeroValue) {
		if (callSite instanceof DefinitionStmt) {
			Value leftOp = ((DefinitionStmt)callSite).getLeftOp();
			return new Transfer<Value>(leftOp, zeroValue);
		}
		return Identity.v();
	}

	@Override
	public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite,
			Value returnSideNode, Value zeroValue) {
		if (callSite instanceof DefinitionStmt) {
			Value leftOp = ((DefinitionStmt)callSite).getLeftOp();
			if (callNode.equivTo(zeroValue) && returnSideNode.equivTo(leftOp)) {
				return new ConstantEdge(BottomValue.v());
			}
		}
		return EdgeIdentity.v();
	}

}
