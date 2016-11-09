package libericc.decider;

import libericc.PredictResult;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public interface Decider {
	public PredictResult branchPredictResult(IfStmt stmt);
	public ResultOfSwitch switchResult(SwitchStmt stmt);
}
