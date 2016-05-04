package hisdroid.callhandler.lifecycle;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.KillAll;
import hisdroid.callhandler.CallHandler;
import hisdroid.edgefunc.GetIntentEdge;
import hisdroid.value.GeneralValue;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;

public class ServiceOnUnbindHandler extends CallHandler {

	@Override
	public Set<MethodSig> getTargets() {
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("android.app.Service", "boolean onUnbind(android.content.Intent)"));
		return targets;
	}

	public FlowFunction<Value> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod, final Value zeroValue){
		final StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
		final Value arg0 = destinationMethod.getActiveBody().getParameterLocal(0);
		
		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				Set<Value> ret = new HashSet<Value>();
				if (source instanceof StaticFieldRef) {
					ret.add(source);
				}
				if (source.equals(iccNo)){
					ret.add(arg0);
				}
				return ret;
			}
		};
	}

	public EdgeFunction<GeneralValue> getCallEdgeFunction(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode, Value zeroValue){
		final StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
		if (srcNode.equivTo(iccNo) && destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode)==0) {
			return new GetIntentEdge(hisdroid.Config.getIccLogs());
		}
		return EdgeIdentity.v();
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, Value zeroValue) {
		return KillAll.v();
	}

	@Override
	public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite,
			Value returnSideNode, Value zeroValue) {
		return EdgeIdentity.v();
	}

}
