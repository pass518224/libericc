package hisdroid.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import heros.InterproceduralCFG;
import hisdroid.Config;
import hisdroid.TriLogic;
import hisdroid.instrumenter.AnalysisInstrumenter;
import hisdroid.instrumenter.AnalysisInstrumenter.ResultOfSwitch;
import hisdroid.value.BottomValue;
import hisdroid.value.BundleValue;
import hisdroid.value.DataValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.NullValue;
import hisdroid.value.PrimitiveDataValue;
import hisdroid.value.StringValue;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LongConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.queue.QueueReader;

public class OneICCEvaluator implements Evaluator {
	static protected final Logger logger = Logger.getLogger("HisDroid");
	
	AnalysisInstrumenter instrumenter;
	InterproceduralCFG<Unit,SootMethod> icfg;
	
	int callSiteCount = 0;
	int reachedCallSite = 0;
	int callArgCount = 0;
	int callArgConstCount = 0;
	int resolvedArgCount = 0;
	int unresolvedArgCount = 0;
	int reachedBranch = 0;
	int unreachedBranch = 0;
	int branchSuccess = 0;
	int branchSuccessWithoutAllPath = 0;
	int branchFailed = 0;
	int branchUnableToDetermine = 0;
	
	public OneICCEvaluator() {
		loadAdbLog();
	}
	
	class MethodArgs {
		public SootMethod sm;
		public String args;
		
		public MethodArgs(SootMethod sm, String args) {
			this.sm = sm;
			this.args = args;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodArgs) {
				return sm.equals(((MethodArgs) o).sm) && args.equals(((MethodArgs) o).args);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(new Object[]{sm, args});
		}
		
		@Override
		public String toString() {
			return "Method = "+sm+", Arg = "+args;
		}
	}
	
	public void evaluate(AnalysisInstrumenter instrumenter) {
		this.instrumenter = instrumenter;
		icfg = new JimpleBasedInterproceduralCFG();
		
		setReachableMethod2args();

		/*logger.finest("Method to hash to Args: "+ method2hash2arg.toString());
		logger.finest("Reachable Method to Args: "+ reachableMethod2args.toString());*/
		
		QueueReader<MethodOrMethodContext> it = Scene.v().getReachableMethods().listener();
		while (it.hasNext()) {
			SootMethod sm = it.next().method();
			if (sm.getDeclaringClass().getName().equals("DummyMain")) continue;
			
			try {
				PatchingChain<Unit> unitChain = sm.retrieveActiveBody().getUnits();
				List<Stmt> branchList = new ArrayList<Stmt>();
				List<Stmt> callSiteList = new ArrayList<Stmt>();
				for (Unit u: unitChain) {
					if (u instanceof IfStmt || u instanceof SwitchStmt) {
						branchList.add((Stmt)u);
					}
					else if (((Stmt)u).containsInvokeExpr()) {
						callSiteList.add((Stmt)u);
					}
				}
				
				for (Stmt callSite: callSiteList) {
					evaluateCallSite(callSite);
				}
				
				for (int branchId=0; branchId < branchList.size(); branchId++) {
					Stmt stmt = branchList.get(branchId);
					if (stmt instanceof IfStmt) {
						EvaluateResult result = evaluateBranch(sm.toString(), (IfStmt)stmt, branchId);
						switch (result) {
						case Success:
							reachedBranch++;
							branchSuccess++;
							if (instrumenter.resultAtBranch((IfStmt)stmt)!=TriLogic.Unknown) {
								branchSuccessWithoutAllPath++;
								logger.finer(String.format("Analysis Success without all path at Branch %s:%d %s", sm, branchId, stmt));
							}
							logger.finer(String.format("Analysis Success at Branch %s:%d %s", sm, branchId, stmt));
							break;
						case Failed:
							reachedBranch++;
							branchFailed++;
							logger.finer(String.format("Analysis Failed at Branch %s:%d %s", sm, branchId, stmt));
							break;
						case UnableToDetermine:
							reachedBranch++;
							branchUnableToDetermine++;
							logger.finer(String.format("Unable to Determine at Branch %s:%d %s", sm, branchId, stmt));
							break;
						case NotReached:
							unreachedBranch++;
						}
					}
					else { // stmt instanceof SwitchStmt
						EvaluateResult result = evaluateSwitch(sm.toString(), (SwitchStmt)stmt, branchId);
						switch (result) {
						case Success:
							branchUnableToDetermine--;
							branchSuccess++;
							it.remove();
							logger.finer(String.format("Analysis Success at Switch %s:%d %s", sm, branchId, stmt));
							break;
						case Failed:
							branchUnableToDetermine--;
							branchFailed++;
							it.remove();
							logger.finer(String.format("Analysis Failed at Switch %s:%d %s", sm, branchId, stmt));
							break;
						case UnableToDetermine:
							logger.finer(String.format("Unable to Determine at Switch %s:%d %s", sm, branchId, stmt));
							break;
						case NotReached:
							unreachedBranch++;
						}
					}
				}
			}
			catch (Exception e) {}
		}

		logger.info("Call Site Count: "+callSiteCount);
		logger.info("Reached Call Site Count: "+reachedCallSite);
		logger.info("Call Arg Count: "+callArgCount);
		logger.info("Call Arg Const Count: "+callArgConstCount);
		logger.info("Resolved Call Arg Count: "+resolvedArgCount);
		logger.info("Unresolved Call Arg Count: "+unresolvedArgCount);
		logger.info("Reached Branches: "+reachedBranch);
		logger.info("Unreached Branches: "+unreachedBranch);
		logger.info("Branch Success: "+branchSuccess);
		logger.info("Branch Success without all path: "+branchSuccessWithoutAllPath);
		logger.info("Branch Failed: "+branchFailed);
		logger.info("Branch Unable to determine: "+branchUnableToDetermine);
	}
	
	void setReachableMethod2args(){
		Queue<MethodArgs> queue = new ArrayDeque<MethodArgs>();
		SootMethod dummyMain = Scene.v().getSootClass("DummyMain").getMethodByName("main");
		
		// Add method to queue
		// traverse dummy main and find icc entrys
		for (Unit u: dummyMain.retrieveActiveBody().getUnits()) {
			if (((Stmt)u).containsInvokeExpr()) {
				logger.finest("invoke: "+u);
				for (SootMethod iccEntry: icfg.getCalleesOfCallAt(u)) {
					logger.finest("\ticcEntry: "+iccEntry);
					String name = iccEntry.getName();
					if (name.equals("<init>")||name.equals("<clinit>")) {
						continue;
					}
					Stmt firstStmt = (Stmt) iccEntry.retrieveActiveBody().getUnits().getFirst();
					Map<Value, GeneralValue> map = instrumenter.getAnalyzer().resultsAt(firstStmt);
					
					// match method args
					Map<Integer, String> hash2arg = method2hash2arg.get(iccEntry.toString());
					if (hash2arg!=null) {
						for (Map.Entry<Integer, String> entry: hash2arg.entrySet()) {
							String args = entry.getValue();
							
							GeneralValue[] gvs = getGvs(map, iccEntry.retrieveActiveBody().getParameterRefs());
							if (matchArgs(gvs, splitArgs(args))) {
								queue.add(new MethodArgs(iccEntry, args));
							}
						}
					}
				}
			}
		}
		
		logger.finest(queue.toString());
		while (!queue.isEmpty()) {
			MethodArgs ma = queue.remove();
			Set<String> args = reachableMethod2args.get(ma.sm.toString());
			if (args == null) {
				args = new HashSet<String>();
				reachableMethod2args.put(ma.sm.toString(), args);
			}
			if (args.contains(ma.args)) continue;
			else {
				args.add(ma.args);
				logger.finest("Add "+ma.toString());
				try {
					for (Unit u: ma.sm.retrieveActiveBody().getUnits()) {
						// for invoke expr
						if (((Stmt)u).containsInvokeExpr()) {
							Stmt callSite = (Stmt) u;
							InvokeExpr ie = callSite.getInvokeExpr();
							Map<Value, GeneralValue> map = instrumenter.getAnalyzer().resultsAt(callSite);
							
							// for each callee
							logger.finest("\t"+u);
							for (SootMethod callee: icfg.getCalleesOfCallAt(u)) {
								logger.finest("\t\t"+callee);
								// match method args
								Map<Integer, String> hash2arg = method2hash2arg.get(callee.toString());
								if (hash2arg==null) continue;
								for (Map.Entry<Integer, String> entry: hash2arg.entrySet()) {
									String argstring = entry.getValue();
									
									GeneralValue[] gvs = getGvs(map, ie.getArgs());
									if (matchArgs(gvs, splitArgs(argstring))) {
										queue.add(new MethodArgs(callee, argstring));
									}
								}
							}
						}
					}
				}
				catch (Exception e) {}
			}
		}
	}
	
	void evaluateCallSite(Stmt callSite){
		Map<Value, GeneralValue> map = instrumenter.getAnalyzer().resultsAt(callSite);
		InvokeExpr ie = callSite.getInvokeExpr();
		GeneralValue[] gvs = getGvs(map, ie.getArgs());
		
		callSiteCount++;

		if (reachableMethod(callSite)) {
			reachedCallSite++;
			for (GeneralValue gv: gvs) {
				callArgCount++;
				if (gv instanceof DataValue) {
					DataValue<?> dv = (DataValue<?>) gv;
					if (dv.bottom()) {
						unresolvedArgCount++;
					}
					else {
						resolvedArgCount++;
					}
				}
				else if (gv==null) {
					callArgConstCount++;
				}
				else /*if (gv instanceof BottomValue)*/ {
					unresolvedArgCount++;
				}
			}
		}
		/*
		boolean[] matchArray = new boolean[ie.getArgCount()];
		for (int i=0; i<matchArray.length; i++) matchArray[i]=true;

		for (SootMethod callee: icfg.getCalleesOfCallAt(callSite)) {
			if (callee.getName().equals("<clinit>")) continue;
			
			Set<String> s = reachableMethod2args.get(callee.toString());
			
			if (s != null) {
				for (String args: s) {
					String[] argArray = splitArgs(args);
					for (int i=0; i<ie.getArgCount(); i++) {
						boolean match;
						if (gvs[i] != null) {
							match = matchArg(gvs[i], argArray[i]);
						}
						else {
							match = matchArg((Constant)ie.getArg(i), argArray[i]);
						}
					}
				}
			}
		}*/
	}

	boolean reachableMethod(Stmt callSite) {
		for (SootMethod callee: icfg.getCalleesOfCallAt(callSite)) {
			if (reachableMethod2args.keySet().contains(callee.toString())) {
				return true;
			}
		}
		return false;
	}
	
	String[] splitArgs(String args) {
		if (args.length()==0) {
			return new String[0];
		}
		
		String delimeter = "\\[Thisisd3mi1eter\\]";
		
		List<String> arglist = new ArrayList<String>();
		Pattern delim = Pattern.compile(delimeter);
    	Matcher matcher = delim.matcher(args);
		int argStart = 0;
    	while (matcher.find()) {
    		int argEnd = matcher.start();
    		arglist.add(args.substring(argStart, argEnd));
    		argStart = matcher.end();
    	}
    	
		return arglist.toArray(new String[arglist.size()]);
	}
	
	GeneralValue[] getGvs(Map<Value, GeneralValue> map, List<Value> args) {
		GeneralValue[] gvs = new GeneralValue[args.size()];
		for (int i=0; i<args.size(); i++) {
			if (args.get(i) instanceof Constant) {
				gvs[i] = null;
			}
			else {
				GeneralValue gv = map.get(args.get(i));
				if (gv == null) gvs[i] = BottomValue.v();
				else gvs[i] = gv;
			}
		}
		return gvs;
	}
	
	boolean matchArgs(GeneralValue[] gvs, String[] argStrings) {
		logger.finest("gvs: "+Arrays.toString(gvs));
		logger.finest("arg: "+Arrays.toString(argStrings));
		if (gvs.length == argStrings.length) {
			for (int i=0; i<gvs.length; i++) {
				if (!matchArg(gvs[i], argStrings[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	boolean matchArg(GeneralValue gv, String argString) {
		if (gv == null || gv instanceof BottomValue) {
			return true;
		}
		else if (gv instanceof NullValue) {
			return argString.equals("null");
		}
		else if (gv instanceof PrimitiveDataValue) {
			if (((PrimitiveDataValue<?>)gv).type() == Integer.class) {
				return ((PrimitiveDataValue<Integer>) gv).valueSet().contains(Integer.parseInt(argString));
			}
			else if (((PrimitiveDataValue<?>)gv).type() == Long.class) {
				return ((PrimitiveDataValue<Long>) gv).valueSet().contains(Long.parseLong(argString));
			}
			else if (((PrimitiveDataValue<?>)gv).type() == Float.class) {
				return ((PrimitiveDataValue<Float>) gv).valueSet().contains(Float.parseFloat(argString));
			}
			else if (((PrimitiveDataValue<?>)gv).type() == Double.class) {
				return ((PrimitiveDataValue<Double>) gv).valueSet().contains(Double.parseDouble(argString));
			}
		}
		else if (gv instanceof StringValue) {
			return ((StringValue) gv).valueSet().contains(argString);
		}
		else if (gv instanceof IntentValue) {
			IntentValue igv = (IntentValue) gv;
			if (igv.bottom()) return true;
			
			Pattern intentPattern = Pattern.compile("Intent \\{ act=(?<action>.*).* \\}BundleStart(?<bundle>.*)BundleStart");
			Matcher matcher = intentPattern.matcher(argString);
			String action, bundle;
			if (matcher.find()) {
				action = matcher.group("action");
				bundle = matcher.group("bundle");
				Map<String, String> map = parseBundle(bundle);
				
				for (JSONObject i: igv.intents()) {
					boolean match = true;
					if (i.getString("Action").equals(action)) {
						for (Map.Entry<String, String> entry: map.entrySet()) {
							try {
								if (i.getJSONObject("mExtras").getJSONObject("map").keySet().contains(entry.getKey())) {
									if (!i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(entry.getKey()).getString("_").equals(entry.getValue())) {
										match = false;
									}
								}
								else match = false;
							}
							catch (JSONException e) { match = false; }
						}
					}
					else {
						match = false;
					}
					if (match) return true;
				}
			}
			return false;
		}
		else if (gv instanceof BundleValue) {
			BundleValue bgv = (BundleValue) gv;
			if (bgv.bottom()) return true;
			
			Map<String, String> map = parseBundle(argString);
			
			for (JSONObject b: bgv.bundles()) {
				boolean match = true;
				for (Map.Entry<String, String> entry: map.entrySet()) {
					try {
						if (b.getJSONObject("map").keySet().contains(entry.getKey())) {
							if (!b.getJSONObject("map").getJSONObject(entry.getKey()).getString("_").equals(entry.getValue())) {
								match = false;
							}
						}
						else match = false;
					}
					catch (JSONException e) { match = false; }
				}
				if (match) return true;
			}
			return false;
		}
		return false;
	}
	
	Map<String, String> parseBundle(String argString) {
		String delimeter = "[BundleDelimeter]";
		Map<String, String> map = new HashMap<String, String>();
		
		int start = 0, end = 0;
		Pattern pattern = Pattern.compile("(.*)->(.*)");
		while (true) {
			end = argString.indexOf(delimeter, start);
			if (end==-1) break;
			
			String s = argString.substring(start ,end);
			Matcher matcher = pattern.matcher(s);
	    	if (matcher.find()) {
	    		String key = matcher.group(1);
	    		String value = matcher.group(2);
	    		map.put(key, value);
	    	}
	    	start = end + 1;
		}
		return map;
	}
	
	boolean matchArg(Constant c, String argString) {
		if (c instanceof IntConstant) {
			int i = ((IntConstant) c).value;
			return i==0 && argString.equals("false") ||
					i==1 && argString.equals("true") ||
					i == Integer.parseInt(argString);
		}
		else if (c instanceof LongConstant) {
			return ((LongConstant) c).value == Long.parseLong(argString);
		}
		else if (c instanceof FloatConstant) {
			return ((FloatConstant) c).value == Float.parseFloat(argString);
		}
		else if (c instanceof DoubleConstant) {
			return ((DoubleConstant) c).value == Double.parseDouble(argString);
		}
		else if (c instanceof StringConstant) {
			return ((StringConstant) c).value.equals(argString);
		}
		return false;
	}
	
	Map<String, Map<Integer, String>> method2hash2arg = new HashMap<String, Map<Integer, String>>(); // method -> (hash -> arguments)
	Map<String, Set<String>> reachableMethod2args = new HashMap<String, Set<String>>(); // method -> arguments
	Map<String, Map<Integer, Map<Integer, Set<Integer>>>> method2branchId2hash2results = new HashMap<String, Map<Integer, Map<Integer, Set<Integer>>>>(); // method -> (branchId -> (hash -> result))
	
	void loadAdbLog() {
		try (BufferedReader br = new BufferedReader(new FileReader(Config.adblogPath))) {
			Pattern patternMethod = Pattern.compile("hisdroid: M: (?<method>.+)#(?<hash>-?\\d+)=\\((?<argument>.*)\\)");
			Pattern patternBranch = Pattern.compile("hisdroid: B: (?<method>.+):(?<branchId>\\d+)#(?<hash>-?\\d+)=(?<result>-?\\d+)");
			
			for (String line; (line = br.readLine()) != null; ) {
		    	Matcher matcher = patternMethod.matcher(line);
		    	if (matcher.find()) {
		    		String method = matcher.group("method");
		    		int hash = Integer.parseInt(matcher.group("hash"));
		    		String arg = matcher.group("argument");
		    		
		    		Map<Integer, String> hash2arg = method2hash2arg.get(method);
		    		if (hash2arg == null) {
		    			hash2arg = new HashMap<Integer, String>();
		    			method2hash2arg.put(method, hash2arg);
		    		}
		    		hash2arg.put(hash, arg);
		    	}
		    	else {
		    		matcher = patternBranch.matcher(line);
		    		if (matcher.find()) {
		    			String method = matcher.group("method");
			    		int branchId = Integer.parseInt(matcher.group("branchId"));
			    		int hash = Integer.parseInt(matcher.group("hash"));
			    		int result = Integer.parseInt(matcher.group("result"));
			    		
			    		Map<Integer, Map<Integer, Set<Integer>>> branchId2hash2results = method2branchId2hash2results.get(method);
			    		if (branchId2hash2results == null) {
			    			branchId2hash2results = new HashMap<Integer, Map<Integer, Set<Integer>>>();
			    			method2branchId2hash2results.put(method, branchId2hash2results);
			    		}
			    		
			    		Map<Integer, Set<Integer>> hash2results = branchId2hash2results.get(branchId);
			    		if (hash2results == null) {
			    			hash2results = new HashMap<Integer, Set<Integer>>();
			    			branchId2hash2results.put(branchId, hash2results);
			    		}
			    		
			    		Set<Integer> results = hash2results.get(hash);
			    		if (results == null) {
			    			results = new HashSet<Integer>();
			    			hash2results.put(hash, results);
			    		}
			    		results.add(result);
			    	}
		    	}
			}
		}
		catch (IOException e) {}
	}
	
	enum EvaluateResult {
		Success, Failed, UnableToDetermine, NotReached;
	}
	

	EvaluateResult evaluateBranch(String method, IfStmt stmt, int branchId) {
		TriLogic analysisResult = instrumenter.resultAtBranch(stmt);

		Set<Integer> runtimeResults = new HashSet<Integer>();
		if (method2branchId2hash2results.containsKey(method) && method2branchId2hash2results.get(method).containsKey(branchId)) {
			Map<Integer, Set<Integer>> hash2results = method2branchId2hash2results.get(method).get(branchId); 
			Set<Integer> reachableHashes = new HashSet<Integer>();
			for (String args: reachableMethod2args.get(method)) {
				reachableHashes.add(args.hashCode());
			}
			for (Map.Entry<Integer, Set<Integer>> entry: hash2results.entrySet()) {
				if (reachableHashes.contains(entry.getKey())) {
					runtimeResults.addAll(entry.getValue());
				}
			}
			
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
		return EvaluateResult.NotReached;
	}
	
	EvaluateResult evaluateSwitch(String method, SwitchStmt stmt, int branchId) {
		Set<Integer> runtimeResults = new HashSet<Integer>();
		Map<Integer, Set<Integer>> hash2results = method2branchId2hash2results.get(method).get(branchId); 
		Set<Integer> reachableHashes = new HashSet<Integer>();
		for (String args: reachableMethod2args.get(method)) {
			reachableHashes.add(args.hashCode());
		}
		for (Map.Entry<Integer, Set<Integer>> entry: hash2results.entrySet()) {
			if (reachableHashes.contains(entry.getKey())) {
				runtimeResults.addAll(entry.getValue());
			}
		}

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
				branchSuccessWithoutAllPath++;
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
}
