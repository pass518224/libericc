package hisdroid.instrumenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import hisdroid.Analyzer;
import hisdroid.TriLogic;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.SwitchStmt;

public class StatsInstrumenter extends Instrumenter {
	int branchId = 0;
	SootMethodRef successMethodRef;
	SootMethodRef failMethodRef;
	
	public StatsInstrumenter(Analyzer analyzer){
		super(analyzer);
		successMethodRef = Scene.v().getSootClass("hisdroid.instrumenter.StatsCounter").getMethodByName("branchSuccess").makeRef();
		failMethodRef = Scene.v().getSootClass("hisdroid.instrumenter.StatsCounter").getMethodByName("branchFail").makeRef();
	}
	
	@Override
	void instrumentBranch(PatchingChain<Unit> unitChain, IfStmt stmt, TriLogic result){
		branchId++;
		logger.fine(String.format("Branch %d: %s", branchId, stmt));
		
		Unit successUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(successMethodRef, IntConstant.v(branchId)));
		Unit failUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(failMethodRef, IntConstant.v(branchId)));
		
		if (result.isTrue()) {
			unitChain.insertBefore(successUnit, stmt.getTarget());
			unitChain.insertAfter(failUnit, stmt);
		}
		else if (result.isFalse()) {
			unitChain.insertAfter(successUnit, stmt);
			unitChain.insertBefore(failUnit, stmt.getTarget());
		}
	}
	
	@Override
	public List<Unit> instrumentSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt, ResultOfSwitch result){
		return Collections.singletonList((Unit)stmt);
		/*Value key = stmt.getKey();
		Map<Integer, Unit> reachableValueToTarget = result.getReachableValueToTarget();
		boolean defaultTargetIsReachable = result.getDefaultTargetIsReachable();

		if (result.bottom) return Collections.singletonList((Unit)stmt);

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
		return newUnits;*/
	}
	
	static public void storeStatsCounter(){
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Object.class.getResourceAsStream("/hisdroid/instrumenter/StatsCounter.class");
			new File("tmp/hisdroid/instrumenter").mkdirs();
			out = new FileOutputStream("tmp/hisdroid/instrumenter/StatsCounter.class");
			            
			int readBytes;
			byte[] buffer = new byte[4096];
			while ((readBytes = in.read(buffer)) > 0) {
				out.write(buffer, 0, readBytes);
			}
		} catch (IOException e) {}
		finally {
			try {
				in.close();
			} catch (IOException e) {}
			try {
				out.close();
			} catch (IOException e) {}
		}
	}
}
