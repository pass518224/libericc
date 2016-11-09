package libericc.relation.decider;

import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class ICCRelationDecider implements Decider {
	IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;

	public ICCRelationDecider(IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver){
		this.solver = solver;
	}
	
	@Override
	public boolean branchResult(IfStmt stmt) {
		Value c = stmt.getCondition();
		if (c instanceof ConditionExpr) {
			ConditionExpr ce = (ConditionExpr) c;
			Value op1 = ce.getOp1();
			Value op2 = ce.getOp2();
			return ((op1 instanceof Constant)||solver.ifdsResultsAt(stmt).contains(op1)) && ((op2 instanceof Constant)||solver.ifdsResultsAt(stmt).contains(op2));
		}
		return false;
	}

	@Override
	public boolean switchResult(SwitchStmt stmt) {
		Value key = stmt.getKey();
		return solver.ifdsResultsAt(stmt).contains(key);
	}

	@Override
	public boolean stmtResult(Stmt stmt) {
		if (stmt instanceof AssignStmt) {
			return solver.ifdsResultsAt(stmt).contains(((AssignStmt) stmt).getLeftOp());
		}
		else if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			if (ie instanceof InstanceInvokeExpr) {
				return solver.ifdsResultsAt(stmt).contains(((InstanceInvokeExpr)ie).getBase());
			}
		}
		return false;
	}
}
