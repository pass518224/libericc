package libericc.pruner;

import libericc.Logger;
import libericc.PredictResult;
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

public class AggressiveBranchPruner extends ConservativeBranchPruner {

	@Override
	public void instrumentBranch(SootMethod method, IfStmt stmt, PredictResult result){
		PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
		if (result.isTrue()) {
			Unit newGoto = Jimple.v().newGotoStmt(stmt.getTarget());
			unitChain.insertBefore(newGoto, stmt);
			unitChain.remove(stmt);
		}
		else if (result.isFalse()) {
			unitChain.remove(stmt);
		}
		else if (result.isAmbiguous() || result.isUnknown()) {
			Type returnType = method.getReturnType();
			Stmt retStmt = null;
			if (returnType instanceof VoidType) {
				retStmt = Jimple.v().newReturnVoidStmt();
			}
			else if (returnType instanceof IntType || returnType instanceof BooleanType) {
				retStmt = Jimple.v().newReturnStmt(IntConstant.v(0));
			}
			else if (returnType instanceof LongType) {
				retStmt = Jimple.v().newReturnStmt(LongConstant.v(0L));
			}
			else if (returnType instanceof FloatType) {
				retStmt = Jimple.v().newReturnStmt(FloatConstant.v(0.0f));
			}
			else if (returnType instanceof DoubleType) {
				retStmt = Jimple.v().newReturnStmt(DoubleConstant.v(0.0));
			}
			else if (returnType instanceof RefLikeType) {
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
}
