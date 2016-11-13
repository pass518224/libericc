package libericc.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import libericc.Config;
import libericc.Logger;
import libericc.PredictResult;
import libericc.decider.Decider;
import libericc.decider.ResultOfSwitch;
import libericc.preevaluate.PreEvaluateInstrumenter;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

/*
 * Default evaluator, conservative
 */

public class NormalEvaluator implements Evaluator {
	Decider decider;
	JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
	
	Map<Integer, Set<Integer>> executedBranch2Value = new HashMap<Integer, Set<Integer>>();
	Set<Integer> executedMethods = new HashSet<Integer>();
	Set<Stmt> executedStmt = new HashSet<Stmt>();
	Set<Stmt> reachableStmt = new HashSet<Stmt>();
	int truePositiveBranch = 0, falsePositiveBranch = 0, falseNegativeBranch = 0, trueNegativeBranch = 0;
	int truePositiveStmt = 0, falsePositiveStmt = 0, falseNegativeStmt = 0, trueNegativeStmt = 0;
	int tt = 0, tf = 0, tb = 0, ta = 0, tu = 0;
	int ft = 0, ff = 0, fb = 0, fa = 0, fu = 0;
	int bt = 0, bf = 0, bb = 0, ba = 0, bu = 0;
	int nt = 0, nf = 0, nb = 0, na = 0, nu = 0;
	int totalStmt = 0, execStmt = 0;
	
	enum EvaluateResult {
		TruePositive, FalsePositive, FalseNegative, TrueNegative;
	}
	
	public NormalEvaluator() {
		loadAdbLog();
		setExecutedStmt();
	}
	
	@Override
	public void evaluate(Decider decider) {
		// set decider
		this.decider = decider;
		// reconstruct control flow graph
		icfg = new JimpleBasedInterproceduralCFG();
		// iterate methods
		for (SootClass sc: Scene.v().getClasses()) {
			for (SootMethod sm: new ArrayList<SootMethod>(sc.getMethods())) {
				if (shouldEvaluate(sm)) {
					setReachableStmts(sm);
					evaluateMethod(sm);
					countStmts(sm);
				}
			}
		}
		// dump to log
		dumpToLog();
	}
	
	boolean shouldEvaluate(SootMethod sm) {
		String packageName = sm.getDeclaringClass().getJavaPackageName();
		return sm.getDeclaringClass().isApplicationClass() &&
				sm.isConcrete() &&
				!packageName.startsWith("android.") &&
				!packageName.startsWith("com.android.") &&
				!packageName.startsWith("libericc.");
	}
	
	void setReachableStmts(SootMethod sm) {
		PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
		// collect all executed code block
		List<Stmt> reachableCodeBlockHead = new ArrayList<Stmt>();
		
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
				case Unknown:
				case Ambiguous:
				case Both:
				default:
					reachableCodeBlockHead.add((Stmt)ifStmt.getTarget());
					reachableCodeBlockHead.add((Stmt)unitChain.getSuccOf(ifStmt));
				}
			}
			else if (u instanceof SwitchStmt) {
				SwitchStmt switchStmt = (SwitchStmt) u;
				ResultOfSwitch result =  decider.switchResult(switchStmt);
				
				if (result.isBottom()) {
					reachableCodeBlockHead.add((Stmt)switchStmt.getDefaultTarget());
					for (Unit target: switchStmt.getTargets()) {
						reachableCodeBlockHead.add((Stmt)target);
					}
				}
				else {
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
	
	void evaluateMethod(SootMethod sm) {
		String methodSig = sm.toString();
		PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
		
		// evaluate branches
		int id = 0;
		for (Unit u: unitChain) {
			if (u instanceof IfStmt) {
				EvaluateResult result = evaluateBranch(methodSig, (IfStmt)u, id);
				Logger.getLogger().fine(String.format("%s at %s in %s", result, u, sm));
				switch (result) {
				case TruePositive:
					truePositiveBranch++;
					break;
				case FalsePositive:
					falsePositiveBranch++;
					break;
				case FalseNegative:
					falseNegativeBranch++;
					break;
				case TrueNegative:
					trueNegativeBranch++;
					break;
				default:
				}
				id++;
			}
			else if (u instanceof SwitchStmt) {
				id++;
			}
		}
		
		// evaluate codes
		for (Unit u: unitChain) {
			if (u instanceof IdentityStmt) {
				continue;
			}
			
			if (reachableStmt.contains((Stmt)u)) {
				if (executedStmt.contains(u)) {
					truePositiveStmt++;
				}
				else {
					falsePositiveStmt++;
				}
			}
			else {
				if (executedStmt.contains(u)) {
					falseNegativeStmt++;
				}
				else {
					trueNegativeStmt++;
				}
			}
		}
	}
	
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
					return EvaluateResult.TruePositive;
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
					return EvaluateResult.FalsePositive;
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
					return EvaluateResult.FalsePositive;
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
				return EvaluateResult.FalsePositive;
			case Both:
			default:
				nb++;
				return EvaluateResult.FalsePositive;
			}
		}
	}
	
	EvaluateResult evaluateSwitch(String method, SwitchStmt stmt, int branchId) {
		return null;
	}
	
	void loadAdbLog() {
		try (BufferedReader br = new BufferedReader(new FileReader(Config.adblogPath))) {
			Pattern patternBranch = Pattern.compile("libericc: B: (?<branchId>-?\\d+):(?<value>-?\\d+):(?<count>-?\\d+)");
			Pattern patternMethod = Pattern.compile("libericc: M: (?<methodId>-?\\d+):(?<count>-?\\d+)");
		    for (String line; (line = br.readLine()) != null; ) {
		    	// branch
		    	Matcher matcher = patternBranch.matcher(line);
		    	if (matcher.find()) {
		    		int branchId = Integer.parseInt(matcher.group("branchId"));
		    		Set<Integer> results = executedBranch2Value.get(branchId);
		    		if (results == null) {
		    			results = new HashSet<Integer>();
		    			executedBranch2Value.put(branchId, results);
		    		}
		    		int result = Integer.parseInt(matcher.group("value"));
		    		results.add(result);
		    		continue;
		    	}
		    	// method
			    matcher = patternMethod.matcher(line);
			    if (matcher.find()) {
		    		int methodId = Integer.parseInt(matcher.group("methodId"));
		    		executedMethods.add(methodId);
		    		continue;
		    	}
		    }
		}
		catch (IOException e) {}
	}
	
	void countStmts(SootMethod sm) {
		PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
		
		for (Unit u: unitChain) {
			if (u instanceof IdentityStmt) {
				continue;
			}
			
			if (executedStmt.contains(u)) {
				execStmt++;
			}
			totalStmt++;
		}
	}
	
	void setExecutedStmt() {
		// for all method of all class
		for (SootClass sc: Scene.v().getClasses()) {
			for (SootMethod sm: new ArrayList<SootMethod>(sc.getMethods())) {
				// is application classes
				if (shouldEvaluate(sm)) {
					// is executed
					if (executedMethods.contains(sm.toString().hashCode())) {
						PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
						// collect all executed code block
						List<Stmt> executedCodeBlockHead = new ArrayList<Stmt>();
						
						// the first stmt is executed
						Unit first = unitChain.getFirst();
						while (first instanceof IdentityStmt) {
							first = unitChain.getSuccOf(first);
						}
						executedCodeBlockHead.add((Stmt)first);
						
						// collect from branch record
						int branchId = 0;
						for (Unit u: unitChain) {
							if (u instanceof IfStmt) {
								IfStmt ifStmt = (IfStmt) u;
								int id = PreEvaluateInstrumenter.branchHashCode(sm, branchId);
								Set<Integer> values = executedBranch2Value.get(id);
								if (values != null) {
									if (values.contains(0)) {
										executedCodeBlockHead.add((Stmt)unitChain.getSuccOf(ifStmt));
									}
									if (values.contains(1)) {
										executedCodeBlockHead.add((Stmt)ifStmt.getTarget());
									}
								}
								branchId++;
							}
							else if (u instanceof SwitchStmt) {
								int id = PreEvaluateInstrumenter.branchHashCode(sm, branchId);
								Set<Integer> values = executedBranch2Value.get(id);
								if (values != null) {
									if (u instanceof TableSwitchStmt) {
										TableSwitchStmt switchStmt = (TableSwitchStmt) u;
										int high = switchStmt.getHighIndex();
										int low = switchStmt.getLowIndex();
										for (int value: values) {
											if (value >= low && value <= high) {
												executedCodeBlockHead.add((Stmt)switchStmt.getTargets().get(value-low));
											}
											else {
												executedCodeBlockHead.add((Stmt)switchStmt.getDefaultTarget());
											}
										}
									}
									else { // LookupSwitchStmt
										LookupSwitchStmt switchStmt = (LookupSwitchStmt) u;
										List<Integer> lookupValues = Lists.transform(switchStmt.getLookupValues(), new Function<IntConstant, Integer>(){
											public Integer apply(IntConstant ic) { return ic.value; }
										});
										for (int value: values) {
											int index = lookupValues.indexOf(value);
											if (index != -1) {
												executedCodeBlockHead.add((Stmt)switchStmt.getTargets().get(index));
											}
											else {
												executedCodeBlockHead.add((Stmt)switchStmt.getDefaultTarget());
											}
										}
									}
									branchId++;
								}
							}
						}
						
						// set executed code from code block
						propageCodes(unitChain, executedCodeBlockHead, executedStmt);
					}
				}
			}
		}
	}
	
	void propageCodes(PatchingChain<Unit> unitChain, Collection<Stmt> codeBlockHead, Set<Stmt> set) {
		for (Stmt stmt: codeBlockHead) {
			Unit it = stmt;
			while (true) {
				if (set.contains((Stmt)it)) {
					break;
				}
				set.add((Stmt)it);
				if (it instanceof IfStmt ||
						it instanceof SwitchStmt ||
						it instanceof ReturnStmt ||
						it instanceof ReturnVoidStmt ||
						it instanceof ThrowStmt)
				{
					break;
				}
				
				if (it instanceof GotoStmt) {
					it = ((GotoStmt) it).getTarget();
				}
				else {
					it = unitChain.getSuccOf(it);
				}
			}
		}
	}
	
	void dumpToLog() {
		// branches
		Logger.getLogger().info("(True, True): "+tt);
		Logger.getLogger().info("(True, False): "+tf);
		Logger.getLogger().info("(True, Both): "+tb);
		Logger.getLogger().info("(True, Ambiguous): "+ta);
		Logger.getLogger().info("(True, Unknown): "+tu);
		
		Logger.getLogger().info("(False, True): "+ft);
		Logger.getLogger().info("(False, False): "+ff);
		Logger.getLogger().info("(False, Both): "+fb);
		Logger.getLogger().info("(False, Ambiguous): "+fa);
		Logger.getLogger().info("(False, Unknown): "+fu);

		Logger.getLogger().info("(Both, True): "+bt);
		Logger.getLogger().info("(Both, False): "+bf);
		Logger.getLogger().info("(Both, Both): "+bb);
		Logger.getLogger().info("(Both, Ambiguous): "+ba);
		Logger.getLogger().info("(Both, Unknown): "+bu);

		Logger.getLogger().info("(None, True): "+nt);
		Logger.getLogger().info("(None, False): "+nf);
		Logger.getLogger().info("(None, Both): "+nb);
		Logger.getLogger().info("(None, Ambiguous): "+na);
		Logger.getLogger().info("(None, Unknown): "+nu);
		// branch results
		Logger.getLogger().info("True Positive Branch: "+truePositiveBranch);
		Logger.getLogger().info("False Positive Branch: "+falsePositiveBranch);
		Logger.getLogger().info("False Negative Branch: "+falseNegativeBranch);
		Logger.getLogger().info("True Negative Branch: "+trueNegativeBranch);
		Logger.getLogger().info("F1 score of Branch: "+((double)(2*truePositiveBranch))/(2*truePositiveBranch+falseNegativeBranch+falsePositiveBranch));
		// stmt results
		Logger.getLogger().info("True Positive Stmt: "+truePositiveStmt);
		Logger.getLogger().info("False Positive Stmt: "+falsePositiveStmt);
		Logger.getLogger().info("False Negative Stmt: "+falseNegativeStmt);
		Logger.getLogger().info("True Negative Stmt: "+trueNegativeStmt);
		Logger.getLogger().info("F1 score of Stmt: "+((double)(2*truePositiveStmt))/(2*truePositiveStmt+falseNegativeStmt+falsePositiveStmt));
		// executed stmt
		Logger.getLogger().info("Executed stmt: "+execStmt);
		Logger.getLogger().info("Total stmt: "+totalStmt);
	}
}
