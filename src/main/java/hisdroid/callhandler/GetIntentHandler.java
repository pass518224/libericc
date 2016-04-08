package hisdroid.callhandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import hisdroid.edgefunc.GetIntentEdge;
import hisdroid.value.GeneralValue;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;

public class GetIntentHandler extends CallHandler {
	JSONObject iccLogs = null;
	
	@Override
	public Set<MethodSig> getTargets(){
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("android.app.Activity", "android.content.Intent getIntent()"));
		return targets;
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, Value zeroValue) {
		if (callSite instanceof DefinitionStmt) {
			final Value lvalue = ((DefinitionStmt)callSite).getLeftOp();
			final StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					if (source.equivTo(iccNo)) {
						Set<Value> ret = new HashSet<Value>();
						ret.add(source);
						ret.add(lvalue);
						return ret;
					}
					if (source.equivTo(lvalue)) {
						return Collections.emptySet();
					}
					return Collections.singleton(source);
				}
			};
		}
		return Identity.v();
	}

	@Override
	public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite,
			Value returnSideNode, Value zeroValue) {
		if (callSite instanceof DefinitionStmt) {
			final Value lvalue = ((DefinitionStmt)callSite).getLeftOp();
			StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
			if (callNode.equivTo(iccNo) && returnSideNode.equivTo(lvalue)) {
				if (iccLogs == null) getIccLogs();
				return new GetIntentEdge(iccLogs);
			}
		}
		return EdgeIdentity.v();
	}
	
	void getIccLogs() {
		iccLogs = hisdroid.Config.getIccLogs();
	}

}
