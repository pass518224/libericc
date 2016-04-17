package hisdroid.callhandler.bundle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import heros.FlowFunction;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import hisdroid.callhandler.CallHandler;
import hisdroid.edgefunc.intent.IntentGetByteEdge;
import hisdroid.value.GeneralValue;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class BundleGetByteHandler extends CallHandler {
	@Override
	public Set<MethodSig> getTargets(){
		Set<MethodSig> targets = new HashSet<MethodSig>();
		targets.add(new MethodSig("android.os.Bundle", "byte getByte(java.lang.String)"));
		targets.add(new MethodSig("android.os.Bundle", "byte getByte(java.lang.String,byte)"));
		return targets;
	}
	
	@Override
	public FlowFunction<Value> getCallToReturnFlowFunction(Unit callSite, Unit returnSite, Value zeroValue) {
		if (callSite instanceof DefinitionStmt) {
			Stmt callStmt = (Stmt) callSite;
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) callStmt.getInvokeExpr();
			final Value base = iie.getBase();
			final Value arg0 = iie.getArg(0);
			final Value lvalue = ((DefinitionStmt)callSite).getLeftOp();
			
			return new FlowFunction<Value>() {
				@Override
				public Set<Value> computeTargets(Value source) {
					if (arg0 instanceof StringConstant) {
						if (source.equivTo(base)) {
							Set<Value> ret = new HashSet<Value>();
							ret.add(source);
							ret.add(lvalue);
							return ret;
						}
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
			Stmt callStmt = (Stmt) callSite;
			final InstanceInvokeExpr iie = (InstanceInvokeExpr) callStmt.getInvokeExpr();
			final Value base = iie.getBase();
			final Value arg0 = iie.getArg(0);
			int argc = iie.getArgCount();
			final Value lvalue = ((DefinitionStmt)callSite).getLeftOp();

			if (callNode.equivTo(base) && returnSideNode.equivTo(lvalue) && arg0 instanceof StringConstant) {
				if (argc == 2) {
					final Value arg1 = iie.getArg(1);
					if (arg1 instanceof IntConstant) {
						return new IntentGetByteEdge(((StringConstant) arg0).value, (byte)((IntConstant) arg1).value);
					}
				}
				return new IntentGetByteEdge(((StringConstant) arg0).value);
			}
		}
		return EdgeIdentity.v();
	}

}
