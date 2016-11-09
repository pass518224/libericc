package libericc.relation.pruner;

import java.util.ArrayList;

import libericc.Logger;
import libericc.relation.decider.Decider;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.ThrowStmt;

public class RemovePruner /*implements Pruner*/ {
	static String dummyFieldClassName = "DummyFieldClass";
	static String dummyFieldName = "dummyField";
	
	SootClass dummyFieldClass;
	SootField dummyField;
	
	public int totalBranch = 0;
	public int totalStmt = 0;
	public int prunedBranch = 0;
	public int prunedStmt = 0;
	
	public RemovePruner() {
		if (!Scene.v().containsClass(dummyFieldClassName)) {
			dummyFieldClass = new SootClass(dummyFieldClassName, Modifier.PUBLIC);
			dummyFieldClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(dummyFieldClass);
			dummyField = new SootField(dummyFieldName, IntType.v(), Modifier.PUBLIC | Modifier.STATIC);
			dummyFieldClass.addField(dummyField);
		}
		else {
			dummyFieldClass = Scene.v().getSootClass(dummyFieldClassName);
			dummyField = dummyFieldClass.getFieldByName(dummyFieldName);
		}
	}

	public void instrumentMethod(Decider decider, SootMethod method) {
		Body body = method.retrieveActiveBody();
		PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
		
		Local dummyInt = Jimple.v().newLocal("DummyIntLocal", IntType.v());
		body.getLocals().add(dummyInt);
		
		Logger.getLogger().finer(String.format("%s", method));
		for (Unit u: new ArrayList<Unit>(unitChain)) {
			Stmt stmt = (Stmt) u;
			if (stmt instanceof IfStmt) {
				totalBranch++;
				Logger.getLogger().fine(String.format("Branch %s in %s", stmt, method));
				if (!decider.branchResult((IfStmt)stmt)) {
					Logger.getLogger().fine(String.format("Prune branch %s in %s", stmt, method));
					pruneBranch(unitChain, (IfStmt)stmt, dummyInt);
				}
			}
			else if (stmt instanceof SwitchStmt) {
				Logger.getLogger().fine(String.format("Switch %s in %s", stmt, method));
				if (!decider.switchResult((SwitchStmt)stmt)) {
					Logger.getLogger().fine(String.format("Prune switch %s in %s", stmt, method));
					pruneSwitch(unitChain, (SwitchStmt)stmt, dummyInt);
				}
			}
			else {
				if (!(stmt instanceof IdentityStmt
					|| stmt instanceof ReturnStmt
					|| stmt instanceof ReturnVoidStmt
					|| stmt instanceof ThrowStmt
					|| stmt instanceof GotoStmt)
				) {
					totalStmt++;
					Logger.getLogger().fine(String.format("Stmt %s in %s", stmt, method));
					if (!decider.stmtResult(stmt)) {
						Logger.getLogger().fine(String.format("Prune stmt %s in %s", stmt, method));
						pruneStmt(unitChain, stmt);
					}
				}
			}
		}
		
		Unit firstStmt = unitChain.getFirst();
		while (firstStmt instanceof IdentityStmt) {
			firstStmt = unitChain.getSuccOf(firstStmt);
		}
		
		for (Local l: body.getLocals()) {
			if (l.getType() instanceof IntType) {
				unitChain.insertBefore(Jimple.v().newAssignStmt(l, Jimple.v().newStaticFieldRef(dummyField.makeRef())), firstStmt);
				Logger.getLogger().fine(String.format("Add %s in %s", l, method));
			}
			else if (l.getType() instanceof RefLikeType) {
				unitChain.insertBefore(Jimple.v().newAssignStmt(l, NullConstant.v()), firstStmt);
				Logger.getLogger().fine(String.format("Add %s in %s", l, method));
			}
		}
	}
	
	//@Override
	public void pruneBranch(PatchingChain<Unit> unitChain, IfStmt stmt, Local dummy) {
		prunedBranch++;
		stmt.setCondition(Jimple.v().newEqExpr(dummy, IntConstant.v(0)));
		return;
	}

	//@Override
	public void pruneSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt, Local dummy) {
		stmt.setKey(dummy);
		return;
	}

	//@Override
	public void pruneStmt(PatchingChain<Unit> unitChain, Stmt stmt) {
		prunedStmt++;
		unitChain.remove(stmt);
		Logger.getLogger().finest(String.format("Prune %s", stmt));
	}

}
