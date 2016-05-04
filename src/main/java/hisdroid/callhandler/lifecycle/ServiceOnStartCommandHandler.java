package hisdroid.callhandler.lifecycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.KillAll;
import hisdroid.callhandler.CallHandler;
import hisdroid.edgefunc.ConstantEdge;
import hisdroid.edgefunc.GetIntentEdge;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValue;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;

public class ServiceOnStartCommandHandler extends CallHandler {

	@Override
	public Set<MethodSig> getTargets() {
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("android.app.Service", "int onStartCommand(android.content.Intent,int,int)"));
		return targets;
	}

	public FlowFunction<Value> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod, final Value zeroValue){
		final List<Value> paramLocals = new ArrayList<Value>();
		final StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
		for (int i=0;i<destinationMethod.getParameterCount();i++) {
			paramLocals.add(destinationMethod.getActiveBody().getParameterLocal(i));
		}
		
		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				Set<Value> ret = new HashSet<Value>();
				if (source.equivTo(zeroValue)) {
					ret.add(paramLocals.get(1));
					ret.add(paramLocals.get(2));
				}
				else {
					if (source instanceof StaticFieldRef) {
						ret.add(source);
					}
					if (source.equals(iccNo)){
						ret.add(paramLocals.get(0));
					}
				}
				return ret;
			}
		};
	}

	public EdgeFunction<GeneralValue> getCallEdgeFunction(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode, Value zeroValue){
		Stmt stmt = (Stmt) callStmt;
		InvokeExpr ie = stmt.getInvokeExpr();
		final StaticFieldRef iccNo = Jimple.v().newStaticFieldRef(Scene.v().getSootClass("DummyMain").getFieldByName("iccNo").makeRef());
		if (srcNode.equivTo(zeroValue) && !destNode.equivTo(zeroValue)) {
			int ind = destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode);
			if (ind == 1 || ind == 2) {
				Value arg = ie.getArgs().get(ind);
				new ConstantEdge(new IntValue(((IntConstant)arg).value));
			}
		}
		else if (srcNode.equivTo(iccNo) && destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode)==0) {
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
