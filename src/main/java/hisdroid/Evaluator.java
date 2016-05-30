package hisdroid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hisdroid.instrumenter.AnalysisInstrumenter;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class Evaluator {
	static protected final Logger logger = Logger.getLogger("HisDroid");
	
	AnalysisInstrumenter instrumenter;
	Map<String, Map<Integer, Set<Integer>>> runtimeResults = new HashMap<String, Map<Integer, Set<Integer>>>(); // method -> (branchId -> (results))
	int unreachedBranch = 0, reachedBranch = 0;
	int reachedBranchSuccess = 0, reachedBranchFailed = 0, reachedBranchUnableToDetermine = 0;
	
	
	public Evaluator(AnalysisInstrumenter instrumenter) {
		this.instrumenter = instrumenter;
	}
	
	public void evaluate() {
		loadAdbLog();
		for (SootClass c: Scene.v().getApplicationClasses()) {
			for (SootMethod s: c.getMethods()) {
				if (instrumenter.shouldInstrument(s)) {
					evaluate(s);
				}
			}
		}
		logger.info("Reached Branch: "+reachedBranch);
		logger.info("Unreached Branch: "+unreachedBranch);
		logger.info("Reached Branch Success: "+reachedBranchSuccess);
		logger.info("Reached Branch Failed: "+reachedBranchFailed);
		logger.info("Reached Branch unable to Determine: "+reachedBranchUnableToDetermine);
	}
	
	void evaluate(SootMethod m) {
		PatchingChain<Unit> unitChain = m.retrieveActiveBody().getUnits();
		
		List<Stmt> stmtList = new ArrayList<Stmt>();
		
		for (Unit u: unitChain) {
			if (u instanceof IfStmt || u instanceof SwitchStmt) {
				logger.finer(String.format("%s:%d %s", m, stmtList.size(), u));
				stmtList.add((Stmt)u);
			}
		}

		for (int i=0; i<stmtList.size(); i++) {
			Stmt s = stmtList.get(i);
			if (s instanceof IfStmt) {
				evaluateBranch(m, (IfStmt)s, i);
			}
			else {
				evaluateSwitch(m, (SwitchStmt)s, i);
			}
		}
	}
	
	void evaluateBranch(SootMethod method, IfStmt stmt, int branchId) {
		TriLogic analysisResult = instrumenter.resultAtBranch(stmt);
		
		Map<Integer, Set<Integer>> branchResults = runtimeResults.get(method.toString());
		if (branchResults != null) {
			Set<Integer> runtimeResults = branchResults.get(branchId);
			if (runtimeResults != null) {
				if (runtimeResults.contains(1) && runtimeResults.contains(0)) {
					if (analysisResult == TriLogic.Unknown) {
						reachedSuccess(method, stmt, branchId);
					}
					else {
						reachedFailed(method, stmt, branchId);
					}
				}
				else if (runtimeResults.contains(1)) {
					if (analysisResult == TriLogic.True) {
						reachedSuccess(method, stmt, branchId);
					}
					else {
						if (analysisResult == TriLogic.Unknown) {
							reachedUnableToDetermine(method, stmt, branchId);
						}
						else {
							reachedFailed(method, stmt, branchId);
						}
					}
				}
				else if (runtimeResults.contains(0)) {
					if (analysisResult == TriLogic.False) {
						reachedSuccess(method, stmt, branchId);
					}
					else {
						if (analysisResult == TriLogic.Unknown) {
							reachedUnableToDetermine(method, stmt, branchId);
						}
						else {
							reachedFailed(method, stmt, branchId);
						}
					}
				}
				return;
			}
		}
		unreached(method, stmt, branchId);
	}
	
	void evaluateSwitch(SootMethod method, SwitchStmt stmt, int branchId) {
		
	}
	
	void unreached(SootMethod method, Stmt stmt, int branchId){
		unreachedBranch++;
	}
	
	void reachedSuccess(SootMethod method, Stmt stmt, int branchId){
		reachedBranch++;
		reachedBranchSuccess++;
	}
	
	void reachedFailed(SootMethod method, Stmt stmt, int branchId){
		reachedBranch++;
		reachedBranchFailed++;
		logger.finest(String.format("Analysis Failed at Reached Branch %s:%d %s", method, branchId, stmt));
	}
	
	void reachedUnableToDetermine(SootMethod method, Stmt stmt, int branchId){
		reachedBranch++;
		reachedBranchUnableToDetermine++;
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
}
