package libericc.pruner;

import java.util.Collections;
import java.util.List;

import libericc.PredictResult;
import libericc.decider.ResultOfSwitch;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public class DoNothingBranchPruner implements BranchPruner {

	@Override
	public void instrumentBranch(SootMethod method, IfStmt stmt, PredictResult result){
		return;
	}
	
	@Override
	public List<Unit> instrumentSwitch(SootMethod method, SwitchStmt stmt, ResultOfSwitch result){
		return Collections.singletonList((Unit)stmt);
	}
}
