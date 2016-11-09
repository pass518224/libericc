package libericc.relation.pruner;

import libericc.Logger;
import soot.BooleanType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class ReturnPruner implements Pruner {
	JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();

	@Override
	public void pruneBranch(PatchingChain<Unit> unitChain, IfStmt stmt) {
		pruneStmt(unitChain, stmt);
	}

	@Override
	public void pruneSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt) {
		pruneStmt(unitChain, stmt);
	}

	@Override
	public void pruneStmt(PatchingChain<Unit> unitChain, Stmt stmt) {
		SootMethod method = icfg.getMethodOf(stmt);
		Type t = method.getReturnType();
		Stmt retStmt = null;
		if (t instanceof VoidType) {
			retStmt = Jimple.v().newReturnVoidStmt();
		}
		else if (t instanceof IntType || t instanceof BooleanType) {
			retStmt = Jimple.v().newReturnStmt(IntConstant.v(0));
		}
		else if (t instanceof LongType) {
			retStmt = Jimple.v().newReturnStmt(LongConstant.v(0L));
		}
		else if (t instanceof FloatType) {
			retStmt = Jimple.v().newReturnStmt(FloatConstant.v(0.0f));
		}
		else if (t instanceof DoubleType) {
			retStmt = Jimple.v().newReturnStmt(DoubleConstant.v(0.0));
		}
		else if (t instanceof RefLikeType) {
			retStmt = Jimple.v().newReturnStmt(NullConstant.v());
		}
		else {
			Logger.getLogger().warning("Not Implement Return Type");
		}
		if (retStmt != null) {
			unitChain.insertBefore(retStmt, stmt);
		}
	}

}
