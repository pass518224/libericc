package hisdroid.callhandler;

import java.util.Set;
import java.util.logging.Logger;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.KillAll;
import hisdroid.value.GeneralValue;
import soot.SootMethod;
import soot.Unit;
import soot.Value;

abstract public class CallHandler {
	static final Logger logger = Logger.getLogger("HisDroid");
	
	public class MethodSig {
		public String className;
		public String subSignature;
		public MethodSig(String className, String subSignature) {
			this.className = className;
			this.subSignature = subSignature;
		}
	}
	
	abstract public Set<MethodSig> getTargets(); 
	
	public FlowFunction<Value> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod, Value zeroValue){
		return KillAll.v();
	}

	public FlowFunction<Value> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite, Value zeroValue){
		return KillAll.v();
	}

	abstract public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, Value zeroValue);
	
	public EdgeFunction<GeneralValue> getCallEdgeFunction(Unit callStmt, Value srcNode, SootMethod destinationMethod, Value destNode, Value zeroValue){
		return EdgeIdentity.v();
	}

	public EdgeFunction<GeneralValue> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Value exitNode, Unit returnSite, Value retNode, Value zeroValue){
		return EdgeIdentity.v();
	}

	abstract public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode, Value zeroValue);
}
