package libericc.instrumenter;

import java.util.ArrayList;
import java.util.List;

import libericc.Logger;
import libericc.PredictResult;
import libericc.decider.Decider;
import libericc.decider.ResultOfSwitch;
import libericc.pruner.BranchPruner;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.util.queue.QueueReader;

public class AnalysisInstrumenter implements Instrumenter {
	Decider decider;
	BranchPruner pruner;
	
	public AnalysisInstrumenter(Decider decider, BranchPruner pruner){
		this.decider = decider;
		this.pruner = pruner;
	}
	
	@Override
	public void instrument(){
		Logger.getLogger().info("Start instrument");
		for (QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();qr.hasNext(); ) {
			SootMethod s = qr.next().method();
			if (shouldInstrument(s)) {
				Logger.getLogger().fine("Instrument "+s.toString());
				instrument(s);
			}
		}
		Logger.getLogger().info("End instrument");
	}
	
	public static boolean shouldInstrument(SootMethod s) {
		String packageName = s.getDeclaringClass().getJavaPackageName();
		return s.getDeclaringClass().isApplicationClass() &&
				s.isConcrete() &&
				!packageName.startsWith("android.") &&
				!packageName.startsWith("com.android.") &&
				!packageName.startsWith("libericc.");
	}
	
	void instrument(SootMethod m) {
		PatchingChain<Unit> unitChain = m.getActiveBody().getUnits();
		
		List<IfStmt> ifList = new ArrayList<IfStmt>();
		List<SwitchStmt> switchList = new ArrayList<SwitchStmt>();
		
		for (Unit u: unitChain) {
			if (u instanceof IfStmt) ifList.add((IfStmt)u);
			else if (u instanceof SwitchStmt) switchList.add((SwitchStmt)u);
		}

		for (SwitchStmt ss: switchList) {
			ResultOfSwitch result = decider.switchResult(ss);
			if (result.isBottom()) {
				Logger.getLogger().fine(String.format("Unknown at Switch: %s", ss));
			}
			else {
				List<Unit> newUnits = pruner.instrumentSwitch(m, ss, result);
				Logger.getLogger().fine(String.format("Change Switch: %s to %s", ss, newUnits));
			}
		}
		for (IfStmt is: ifList) {
			PredictResult result = decider.branchPredictResult(is);
			Logger.getLogger().fine(String.format("%s at branch: %s", result, is));
			pruner.instrumentBranch(m, is, result);
		}
	}
}
