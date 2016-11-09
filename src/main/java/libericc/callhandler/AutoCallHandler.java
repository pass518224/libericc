package libericc.callhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.KillAll;
import libericc.callhandler.CallHandler;
import libericc.edgefunc.ConstantEdge;
import libericc.edgefunc.GetIntentEdge;
import libericc.flowfunc.KillAllExceptStaticField;
import libericc.value.GeneralValue;
import libericc.value.NullValue;
import libericc.value.PrimitiveDataValue;
import libericc.value.StringValue;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

/*
 * The call handler used for ICCs to system_server
 */
public class AutoCallHandler extends CallHandler {

	private AutoCallHandler() {}
	
	static AutoCallHandler instance = new AutoCallHandler();
	
	public static AutoCallHandler v() {
		return instance;
	}
	
	@Override
	public Set<MethodSig> getTargets() {
		return Collections.emptySet();
	}

	public FlowFunction<Value> getCallFlowFunction(Unit callStmt, final SootMethod destinationMethod, final Value zeroValue){
		Stmt stmt = (Stmt) callStmt;
		final InvokeExpr ie = stmt.getInvokeExpr();
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
					for (int i=0;i<destinationMethod.getParameterCount();i++) {
						String type = destinationMethod.getParameterType(i).toString();
						if (type.equals("int") || type.equals("boolean") || type.equals("java.lang.String)")) {
							if (ie.getArg(i) instanceof Constant) {
								ret.add(paramLocals.get(i));
							}
						}
					}
				}
				else {
					if (source instanceof StaticFieldRef) {
						ret.add(source);
					}
					if (source.equals(iccNo)){
						for (int i=0;i<destinationMethod.getParameterCount();i++) {
							String type = destinationMethod.getParameterType(i).toString();
							if (type.equals("android.content.Intent")) {
								ret.add(paramLocals.get(i));
							}
						}
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
			if (ind != -1) {
				Value v = ie.getArgs().get(ind);
				switch (destinationMethod.getParameterType(ind).toString()) {
				case "int":
				case "boolean":
					if (v instanceof IntConstant) {
						return new ConstantEdge(new PrimitiveDataValue<Integer>(Integer.class, ((IntConstant)ie.getArgs().get(ind)).value));
					}
					break;
				case "java.lang.String":
					if (v instanceof StringConstant) {
						return new ConstantEdge(new StringValue(((StringConstant)v).value));
					}
					else if (v instanceof NullConstant) {
						return new ConstantEdge(new NullValue());
					}
				}
			}
		}
		else if (srcNode.equivTo(iccNo)) {
			int ind = destinationMethod.getActiveBody().getParameterLocals().indexOf(destNode);
			if (ind != -1) {
				if (destinationMethod.getParameterType(ind).toString().equals("android.content.Intent")) {
					return new GetIntentEdge(libericc.Config.getIccLogs());
				}
			}
		}
		return EdgeIdentity.v();
	}
	
	@Override
	public FlowFunction<Value> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite, Value zeroValue){
		return KillAllExceptStaticField.v();
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
