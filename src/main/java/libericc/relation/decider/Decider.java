package libericc.relation.decider;

import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public interface Decider {
	public boolean branchResult(IfStmt stmt);
	public boolean switchResult(SwitchStmt stmt);
	public boolean stmtResult(Stmt stmt);
}
