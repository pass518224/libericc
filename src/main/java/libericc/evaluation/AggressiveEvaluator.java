package libericc.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import libericc.PredictResult;
import libericc.decider.ResultOfSwitch;
import libericc.preevaluate.PreEvaluateInstrumenter;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class AggressiveEvaluator extends NormalEvaluator {
	
	@Override
	void setReachableStmts(SootMethod sm) {
		PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
		// collect all executed code block
		List<Stmt> reachableCodeBlockHead = new ArrayList<Stmt>();
		
		if (!Scene.v().getReachableMethods().contains(sm)) return;
		
		// first stmt is reachable
		Unit first = unitChain.getFirst();
		while (first instanceof IdentityStmt) {
			first = unitChain.getSuccOf(first);
		}
		reachableCodeBlockHead.add((Stmt)first);
		
		// collect from branch record
		for (Unit u: unitChain) {
			if (u instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) u;
				PredictResult analysisResult = decider.branchPredictResult(ifStmt);
				switch (analysisResult) {
				case True:
					reachableCodeBlockHead.add((Stmt)ifStmt.getTarget());
					break;
				case False:
					reachableCodeBlockHead.add((Stmt)unitChain.getSuccOf(ifStmt));
					break;
				case Both:
					reachableCodeBlockHead.add((Stmt)ifStmt.getTarget());
					reachableCodeBlockHead.add((Stmt)unitChain.getSuccOf(ifStmt));
					break;
				case Unknown:
				case Ambiguous:
				default:
				}
			}
			else if (u instanceof SwitchStmt) {
				SwitchStmt switchStmt = (SwitchStmt) u;
				ResultOfSwitch result =  decider.switchResult(switchStmt);
				
				if (!result.isBottom()) {
					if (result.getDefaultTargetIsReachable()) {
						reachableCodeBlockHead.add((Stmt)switchStmt.getDefaultTarget());
					}
					for (Unit target: result.getReachableValueToTarget().values()) {
						reachableCodeBlockHead.add((Stmt)target);
					}
				}
			}
		}
		
		// set reachable code from code block
		propageCodes(unitChain, reachableCodeBlockHead, reachableStmt);
	}
	
	@Override
	EvaluateResult evaluateBranch(String method, IfStmt stmt, int branchId) {
		PredictResult analysisResult = decider.branchPredictResult(stmt);
		
		int id = PreEvaluateInstrumenter.branchHashCode(method, branchId);
		
		if (executedBranch2Value.containsKey(id)) {
			Set<Integer> runtimeResults = executedBranch2Value.get(id);
			// both
			if (runtimeResults.contains(1) && runtimeResults.contains(0)) {
				switch (analysisResult) {
				case True:
					bt++;
					return EvaluateResult.FalseNegative;
				case False:
					bf++;
					return EvaluateResult.FalseNegative;
				case Unknown:
					bu++;
					return EvaluateResult.FalseNegative;
				case Ambiguous:
					ba++;
					return EvaluateResult.FalseNegative;
				case Both:
				default:
					bb++;
					return EvaluateResult.TruePositive;
				}
			}
			// True
			else if (runtimeResults.contains(1)) {
				switch (analysisResult) {
				case True:
					tt++;
					return EvaluateResult.TruePositive;
				case False:
					tf++;
					return EvaluateResult.FalseNegative;
				case Unknown:
					tu++;
					return EvaluateResult.FalseNegative;
				case Ambiguous:
					ta++;
					return EvaluateResult.FalseNegative;
				case Both:
				default:
					tb++;
					return EvaluateResult.FalsePositive;
				}
			}
			// False
			else /*if (runtimeResults.contains(0))*/ { // always true
				switch (analysisResult) {
				case False:
					ff++;
					return EvaluateResult.TruePositive;
				case True:
					ft++;
					return EvaluateResult.FalseNegative;
				case Unknown:
					fu++;
					return EvaluateResult.FalseNegative;
				case Ambiguous:
					fa++;
					return EvaluateResult.FalseNegative;
				case Both:
				default:
					fb++;
					return EvaluateResult.FalsePositive;
				}
			}
		}
		// both not taken
		else {
			switch (analysisResult) {
			case Unknown:
				nu++;
				return EvaluateResult.TrueNegative;
			case True:
				nt++;
				return EvaluateResult.FalsePositive;
			case False:
				nf++;
				return EvaluateResult.FalsePositive;
			case Ambiguous:
				na++;
				return EvaluateResult.TrueNegative;
			case Both:
			default:
				nb++;
				return EvaluateResult.FalsePositive;
			}
		}
	}
}
