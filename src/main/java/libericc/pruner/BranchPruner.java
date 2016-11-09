package libericc.pruner;

import java.util.List;

import libericc.PredictResult;
import libericc.decider.ResultOfSwitch;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public interface BranchPruner {
	void instrumentBranch(SootMethod method, IfStmt stmt, PredictResult result);
	public List<Unit> instrumentSwitch(SootMethod method, SwitchStmt stmt, ResultOfSwitch result);
}
