package libericc.decider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.JSONObject;

import heros.InterproceduralCFG;
import libericc.PredictResult;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class LoadJsonDecider implements Decider {
	JSONObject results;
	InterproceduralCFG<Unit,SootMethod> icfg = new JimpleBasedInterproceduralCFG();
	
	public LoadJsonDecider(String filename) throws FileNotFoundException {
		Scanner s = new Scanner(new File(filename));
		String content = s.useDelimiter("\\Z").next();
		s.close();
		results = new JSONObject(content);
	}
	
	public PredictResult branchPredictResult(IfStmt stmt){
		SootMethod method = icfg.getMethodOf(stmt);
		try {
			JSONObject methodResult = results.getJSONObject(method.toString());
			PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
			int i=0;
			for (Unit u: unitChain) {
				if (u == stmt) {
					return PredictResult.parseString(methodResult.getString(Integer.toString(i)));
				}
				if (u instanceof IfStmt || u instanceof SwitchStmt) {
					i++;
				}
			}
		}
		catch (Exception e) {}
		return PredictResult.Unknown;
	}

	@Override
	public ResultOfSwitch switchResult(SwitchStmt stmt) {
		SootMethod method = icfg.getMethodOf(stmt);
		try {
			JSONObject methodResult = results.getJSONObject(method.toString());
			PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
			int i=0;
			for (Unit u: unitChain) {
				if (u == stmt) {
					return new ResultOfSwitch(stmt, methodResult.getJSONObject(Integer.toString(i)));
				}
				if (u instanceof IfStmt || u instanceof SwitchStmt) {
					i++;
				}
			}
		}
		catch (Exception e) {}
		return new ResultOfSwitch();
	}
}
