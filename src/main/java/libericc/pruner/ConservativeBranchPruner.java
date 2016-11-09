package libericc.pruner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libericc.PredictResult;
import libericc.decider.ResultOfSwitch;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.SwitchStmt;

public class ConservativeBranchPruner implements BranchPruner {

	@Override
	public void instrumentBranch(SootMethod method, IfStmt stmt, PredictResult result){
		PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
		if (result.isTrue()) {
			Unit newGoto = Jimple.v().newGotoStmt(stmt.getTarget());
			unitChain.insertBefore(newGoto, stmt);
			unitChain.remove(stmt);
		}
		else if (result.isFalse()) {
			unitChain.remove(stmt);
		}
	}
	
	@Override
	public List<Unit> instrumentSwitch(SootMethod method, SwitchStmt stmt, ResultOfSwitch result){
		PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
		Value key = stmt.getKey();
		Map<Integer, Unit> reachableValueToTarget = result.getReachableValueToTarget();
		boolean defaultTargetIsReachable = result.getDefaultTargetIsReachable();

		if (result.isBottom()) return Collections.singletonList((Unit)stmt);

		int pathCount = reachableValueToTarget.size();
		if (defaultTargetIsReachable) pathCount++;
		List<Unit> newUnits = new ArrayList<Unit>();
		
		// return a gotoStmt if only 1 target is reachable
		if (pathCount == 1) {
			if (defaultTargetIsReachable) {
				newUnits.add(Jimple.v().newGotoStmt(stmt.getDefaultTarget()));
			}
			else {
				newUnits.add(Jimple.v().newGotoStmt(reachableValueToTarget.entrySet().iterator().next().getValue()));
			}
		}
		// return a ifStmt if just 2 targets are reachable
		else if (pathCount == 2) {
			// if key == targetValue goto target; else goto default target
			if (defaultTargetIsReachable) {
				Entry<Integer, Unit> pair = reachableValueToTarget.entrySet().iterator().next();
				int targetValue = pair.getKey();
				Unit target = pair.getValue();
				newUnits.add(Jimple.v().newIfStmt(Jimple.v().newEqExpr(key, IntConstant.v(targetValue)), target));
				newUnits.add(Jimple.v().newGotoStmt(stmt.getDefaultTarget()));
			}
			// if key == targetValue1 goto target1; else goto target2
			else {
				Iterator<Entry<Integer, Unit>> it = reachableValueToTarget.entrySet().iterator();
				Entry<Integer, Unit> pair = it.next();
				int targetValue1 = pair.getKey();
				Unit target1 = pair.getValue();
				Unit target2 = it.next().getValue();
				newUnits.add(Jimple.v().newIfStmt(Jimple.v().newEqExpr(key, IntConstant.v(targetValue1)), target1));
				newUnits.add(Jimple.v().newGotoStmt(target2));
			}
		}
		// return a new lookupSwitchStmt
		else if (pathCount >= 3) {
			List<IntConstant> lookupValues = new ArrayList<IntConstant>();
			List<Unit> targets = new ArrayList<Unit>();
			for (Entry<Integer, Unit> e: reachableValueToTarget.entrySet()) {
				lookupValues.add(IntConstant.v(e.getKey()));
				targets.add(e.getValue());
			}
			// use original default target
			if (defaultTargetIsReachable) {
				newUnits.add(Jimple.v().newLookupSwitchStmt(key, lookupValues, targets, stmt.getDefaultTarget()));
			}
			// choose the last target as default
			else {
				lookupValues.remove(lookupValues.size()-1);
				Unit defaultTarget = targets.remove(targets.size()-1);
				newUnits.add(Jimple.v().newLookupSwitchStmt(key, lookupValues, targets, defaultTarget));
			}
		}
		if (!newUnits.isEmpty()) {
			unitChain.insertBefore(newUnits, stmt);
			unitChain.remove(stmt);
		}
		return newUnits;
	}
}
