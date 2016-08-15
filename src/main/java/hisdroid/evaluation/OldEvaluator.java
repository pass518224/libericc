package hisdroid.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hisdroid.Config;
import hisdroid.TriLogic;
import hisdroid.instrumenter.AnalysisInstrumenter;
import hisdroid.instrumenter.AnalysisInstrumenter.ResultOfSwitch;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;

public class OldEvaluator {
	static protected final Logger logger = Logger.getLogger("HisDroid");
	
	AnalysisInstrumenter instrumenter;
	Map<String, Map<Integer, Set<Integer>>> runtimeResults = new HashMap<String, Map<Integer, Set<Integer>>>(); // method -> (branchId -> (results))
	Map<String, Map<Integer, Stmt>> reachedBranches = new HashMap<String, Map<Integer, Stmt>>(); // method -> (branchId -> branch)
	Map<String, Map<Integer, Stmt>> undecidedBranches = new HashMap<String, Map<Integer, Stmt>>(); // method -> (branchId -> branch)
	int unreachedBranch = 0, reachedBranchSuccess = 0, reachedBranchFailed = 0, reachedBranchUnableToDetermine = 0;
	int reachedBranchSuccessWithoutAllPath = 0;
	
	enum EvaluateResult {
		Success, Failed, UnableToDetermine;
	}
	
	public OldEvaluator() {
		loadAdbLog();
		setReachedBranches();
	}
	
	public void evaluate(AnalysisInstrumenter instrumenter) {
		this.instrumenter = instrumenter;
		for (String method: undecidedBranches.keySet()) {
			Map<Integer, Stmt> branchIdTobranches = undecidedBranches.get(method);
			for (Iterator<Entry<Integer, Stmt>> it = branchIdTobranches.entrySet().iterator(); it.hasNext(); ) {
				Entry<Integer, Stmt> entry = it.next();
				int branchId = entry.getKey();
				Stmt stmt = entry.getValue();
				if (stmt instanceof IfStmt) {
					EvaluateResult result = evaluateBranch(method, (IfStmt)stmt, branchId);
					switch (result) {
					case Success:
						reachedBranchUnableToDetermine--;
						reachedBranchSuccess++;
						if (instrumenter.resultAtBranch((IfStmt)stmt)!=TriLogic.Unknown) {
							reachedBranchSuccessWithoutAllPath++;
						}
						it.remove();
						logger.finer(String.format("Analysis Success at Branch %s:%d %s", method, branchId, stmt));
						break;
					case Failed:
						reachedBranchUnableToDetermine--;
						reachedBranchFailed++;
						it.remove();
						logger.finer(String.format("Analysis Failed at Branch %s:%d %s", method, branchId, stmt));
						break;
					case UnableToDetermine:
						logger.finer(String.format("Unable to Determine at Branch %s:%d %s", method, branchId, stmt));
						break;
					}
				}
				else {
					EvaluateResult result = evaluateSwitch(method, (SwitchStmt)stmt, branchId);
					switch (result) {
					case Success:
						reachedBranchUnableToDetermine--;
						reachedBranchSuccess++;
						it.remove();
						logger.finer(String.format("Analysis Success at Switch %s:%d %s", method, branchId, stmt));
						break;
					case Failed:
						reachedBranchUnableToDetermine--;
						reachedBranchFailed++;
						it.remove();
						logger.finer(String.format("Analysis Failed at Switch %s:%d %s", method, branchId, stmt));
						break;
					case UnableToDetermine:
						logger.finer(String.format("Unable to Determine at Switch %s:%d %s", method, branchId, stmt));
						break;
					}
				}
			}
		}
		logger.info("Reached Branch: "+(reachedBranchSuccess+reachedBranchFailed+reachedBranchUnableToDetermine));
		logger.info("Unreached Branch: "+unreachedBranch);
		logger.info("Reached Branch Success: "+reachedBranchSuccess);
		logger.info("Reached Branch Success w/o all path reached: "+reachedBranchSuccessWithoutAllPath);
		logger.info("Reached Branch Failed: "+reachedBranchFailed);
		logger.info("Reached Branch unable to Determine: "+reachedBranchUnableToDetermine);
		logger.info("Reached Branches: "+reachedBranches);
	}
	
	
	EvaluateResult evaluateBranch(String method, IfStmt stmt, int branchId) {
		TriLogic analysisResult = instrumenter.resultAtBranch(stmt);
		
		Map<Integer, Set<Integer>> branchResults = runtimeResults.get(method);
		Set<Integer> runtimeResults = branchResults.get(branchId);
		if (runtimeResults.contains(1) && runtimeResults.contains(0)) {
			if (analysisResult == TriLogic.Unknown) {
				return EvaluateResult.Success;
			}
			else {
				return EvaluateResult.Failed;
			}
		}
		else if (runtimeResults.contains(1)) {
			if (analysisResult == TriLogic.True) {
				return EvaluateResult.Success;
			}
			else {
				if (analysisResult == TriLogic.Unknown) {
					return EvaluateResult.UnableToDetermine;
				}
				else {
					return EvaluateResult.Failed;
				}
			}
		}
		else /*if (runtimeResults.contains(0))*/ { // always true
			if (analysisResult == TriLogic.False) {
				return EvaluateResult.Success;
			}
			else {
				if (analysisResult == TriLogic.Unknown) {
					return EvaluateResult.UnableToDetermine;
				}
				else {
					return EvaluateResult.Failed;
				}
			}
		}
	}
	
	EvaluateResult evaluateSwitch(String method, SwitchStmt stmt, int branchId) {
		Map<Integer, Set<Integer>> branchResults = runtimeResults.get(method);
		Set<Integer> runtimeResults = branchResults.get(branchId);

		boolean allTargetReached;
		boolean defaultTargetReached = false;
		Set<Integer> reachedTarget = new HashSet<Integer>();
		
		if (stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt lookupStmt = (LookupSwitchStmt) stmt;
			
			Set<Integer> lookupValues = new HashSet<Integer>();
			for (IntConstant lookupValue: lookupStmt.getLookupValues()) {
				lookupValues.add(lookupValue.value);
			}
			
			for (int runtimeValue: runtimeResults) {
				if (lookupValues.contains(runtimeValue)) {
					reachedTarget.add(runtimeValue);
				}
				else {
					defaultTargetReached = true;
				}
			}
			
			allTargetReached = reachedTarget.size() == lookupValues.size();
		}
		else { // stmt instanceof TableSwitchStmt
			TableSwitchStmt tableStmt = (TableSwitchStmt) stmt;
			
			int highIndex = tableStmt.getHighIndex();
			int lowIndex = tableStmt.getLowIndex();
			
			for (int runtimeValue: runtimeResults) {
				if (runtimeValue <= highIndex && runtimeValue >= lowIndex) {
					reachedTarget.add(runtimeValue);
				}
				else {
					defaultTargetReached = true;
				}
			}
			
			allTargetReached = reachedTarget.size() == highIndex-lowIndex+1;
		}

		ResultOfSwitch analysisResult = instrumenter.resultAtSwitch(stmt);

		if (analysisResult.isBottom()) {
			if (allTargetReached && defaultTargetReached) {
				reachedBranchSuccessWithoutAllPath++;
				return EvaluateResult.Success;
			}
			else return EvaluateResult.UnableToDetermine;
		}
		else {
			Set<Integer> analysisTarget = analysisResult.getReachableValueToTarget().keySet();
			boolean analysisDefaultTargetReached = analysisResult.getDefaultTargetIsReachable();
			if (analysisTarget.equals(reachedTarget) && analysisDefaultTargetReached == defaultTargetReached) { // exactly match
				return EvaluateResult.Success;
			}
			else if (analysisTarget.containsAll(reachedTarget) && (analysisDefaultTargetReached || !defaultTargetReached)) { // analysis result > runtime result
				return EvaluateResult.UnableToDetermine;
			}
			else {
				return EvaluateResult.Failed;
			}
		}
	}
	
	void loadAdbLog() {
		try (BufferedReader br = new BufferedReader(new FileReader(Config.adblogPath))) {
			Pattern pattern = Pattern.compile("hisdroid: (?<result>True|False|-?\\d+) at (branch|switch) (?<method>.*):(?<branchId>\\d+)");
		    for (String line; (line = br.readLine()) != null; ) {
		    	Matcher matcher = pattern.matcher(line);
		    	if (matcher.find()) {
		    		Map<Integer, Set<Integer>> branchResults = runtimeResults.get(matcher.group("method"));
		    		if (branchResults == null) {
		    			branchResults = new HashMap<Integer, Set<Integer>>();
		    			runtimeResults.put(matcher.group("method"), branchResults);
		    		}
		    		int branchId = Integer.parseInt(matcher.group("branchId"));
		    		Set<Integer> results = branchResults.get(branchId);
		    		if (results == null) {
		    			results = new HashSet<Integer>();
		    			branchResults.put(branchId, results);
		    		}
		    		String resultString = matcher.group("result");
		    		int result;
		    		if (resultString.equals("True")) result = 1;
		    		else if (resultString.equals("False")) result = 0;
		    		else result = Integer.parseInt(resultString);
		    		results.add(result);
		    	}
		    }
		}
		catch (IOException e) {}
	}
	
	@SuppressWarnings("unchecked")
	void setReachedBranches() {
		for (SootClass c: Scene.v().getApplicationClasses()) {
			for (SootMethod method: c.getMethods()) {
				if (Scene.v().getReachableMethods().contains(method)&&AnalysisInstrumenter.shouldInstrument(method)) {
					String methodSig = method.toString();
					
					PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
					List<Stmt> stmtList = new ArrayList<Stmt>();
					for (Unit u: unitChain) {
						if (u instanceof IfStmt || u instanceof SwitchStmt) {
							stmtList.add((Stmt)u);
						}
					}
					unreachedBranch += stmtList.size();
					
					if (runtimeResults.containsKey(methodSig)) {
						HashMap<Integer, Stmt> methodBranches = new HashMap<Integer, Stmt>();
						for (int branchId: runtimeResults.get(methodSig).keySet()) {
							methodBranches.put(branchId, stmtList.get(branchId));
						}
						reachedBranches.put(methodSig, methodBranches);
						undecidedBranches.put(methodSig, (Map<Integer, Stmt>)methodBranches.clone());
						
						unreachedBranch -= methodBranches.size();
						reachedBranchUnableToDetermine += methodBranches.size();
					}
				}
			}
		}
	}
}
