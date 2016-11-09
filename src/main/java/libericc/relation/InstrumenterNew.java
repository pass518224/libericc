package libericc.relation;

import java.util.ArrayList;
import java.util.List;

import libericc.Logger;
import libericc.relation.decider.Decider;
import libericc.relation.pruner.Pruner;
import libericc.relation.pruner.RemovePruner;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.ThrowStmt;
import soot.util.queue.QueueReader;

public class InstrumenterNew {
	Decider decider;
	RemovePruner pruner;
	
	public InstrumenterNew(Decider decider, RemovePruner pruner){
		this.decider = decider;
		this.pruner = pruner;
	}
	
	public void instrument(){
		/*for (QueueReader<MethodOrMethodContext> it = Scene.v().getReachableMethods().listener();it.hasNext();) {
			SootMethod sm = it.next().method();
			if (shouldInstrument(sm)) {
				instrument(sm);
			}
		}*/
		List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
		for (SootClass sc: lsc) {
			List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
			for (SootMethod s: lsm) {
				if (shouldInstrument(s)) {
					instrument(s);
				}
			}
		}
		Logger.getLogger().finest(String.format("Total Branch = %d", pruner.totalBranch));
		Logger.getLogger().finest(String.format("Pruned Branch = %d", pruner.prunedBranch));
		Logger.getLogger().finest(String.format("Total Stmt = %d", pruner.totalStmt));
		Logger.getLogger().finest(String.format("Pruned Stmt = %d", pruner.prunedStmt));
	}
	
	boolean shouldInstrument(SootMethod s) {
		String className = s.getDeclaringClass().getName();
		return !(className.startsWith("android")||className.startsWith("com.android."))
				&& s.isConcrete()
				&& s.getDeclaringClass().isApplicationClass();
	}
	
	void instrument(SootMethod m) {
		try {
			pruner.instrumentMethod(decider, m);
		}
		catch (Exception e) {
			Logger.getLogger().warning(e.toString());
		}
	}
}
