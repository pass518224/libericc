package hisdroid.callhandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import hisdroid.edgefunc.ConstantEdge;
import hisdroid.value.GeneralValue;
import hisdroid.value.StringValue;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class StringInitHandler extends CallHandler {
	@Override
	public Set<MethodSig> getTargets(){
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("java.lang.String", "void <init>(java.lang.String)"));
		return targets;
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, final Value zeroValue){
		Stmt callStmt = (Stmt) callSite;
		final InstanceInvokeExpr iie = (InstanceInvokeExpr) callStmt.getInvokeExpr();
		final Value base = iie.getBase();
		final Value arg0 = iie.getArg(0);
		
		return new FlowFunction<Value>() {
			@Override
			public Set<Value> computeTargets(Value source) {
				if (arg0 instanceof StringConstant) {
					if (source == zeroValue) {
						return Collections.singleton(base);
					}
					if (source.equivTo(base)) {
						return Collections.emptySet();
					}
				}
				else {
					if (source.equivTo(arg0)) {
						Set<Value> ret = new HashSet<Value>();
						ret.add(source);
						ret.add(base);
						return ret;
					}
					if (source.equivTo(base)) {
						return Collections.emptySet();
					}
				}
				return Collections.singleton(source);
			}
		};
	}
	
	@Override
	public EdgeFunction<GeneralValue> getCallToReturnEdgeFunction(Unit callSite, Value callNode, Unit returnSite, Value returnSideNode, Value zeroValue){
		Stmt callStmt = (Stmt) callSite;
		final InstanceInvokeExpr iie = (InstanceInvokeExpr) callStmt.getInvokeExpr();
		final Value base = iie.getBase();
		final Value arg0 = iie.getArg(0);

		if (arg0 instanceof StringConstant) {
			if (callNode == zeroValue && returnSideNode.equivTo(base)) {
				return new ConstantEdge(new StringValue(((StringConstant) arg0).value));
			}
		}
		
		return EdgeIdentity.v();
	}
}
