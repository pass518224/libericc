package libericc.relation.pruner;

import soot.PatchingChain;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public interface Pruner {
	void pruneBranch(PatchingChain<Unit> unitChain, IfStmt stmt);
	void pruneSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt);
	void pruneStmt(PatchingChain<Unit> unitChain, Stmt stmt);
}
