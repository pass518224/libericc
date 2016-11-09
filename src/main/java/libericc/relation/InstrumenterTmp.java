package libericc.relation;

import java.util.ArrayList;
import java.util.List;

import libericc.Logger;
import libericc.relation.decider.Decider;
import libericc.relation.pruner.Pruner;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public class InstrumenterTmp {
	Pruner pruner;
	Decider decider;
	
	public InstrumenterTmp(Decider decider, Pruner pruner){
		this.decider = decider;
		this.pruner = pruner;
	}
	
	public void instrument(){
		List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
		for (SootClass sc: lsc) {
			List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
			for (SootMethod s: lsm) {
				if (shouldInstrument(s)) {
					instrument(s);
				}
			}
		}
	}
	
	boolean shouldInstrument(SootMethod s) {
		String className = s.getDeclaringClass().getName();
		return !(className.startsWith("android")||className.startsWith("com.android."))
				&& s.isConcrete()
				&& s.getDeclaringClass().isApplicationClass();
	}
	
	void instrument(SootMethod m) {
		try {
			PatchingChain<Unit> unitChain = m.retrieveActiveBody().getUnits();
			Logger.getLogger().finest(String.format("%s", m));
			for (Unit u: unitChain) {
				if (u instanceof IfStmt) {
					if (!decider.branchResult((IfStmt)u)) {
						pruner.pruneBranch(unitChain, (IfStmt)u);
					}
				}
				else if (u instanceof SwitchStmt) {
					if (!decider.switchResult((SwitchStmt)u)) {
						pruner.pruneSwitch(unitChain, (SwitchStmt)u);
					}
				}
			}
		}
		catch (Exception e) {}
	}
}
