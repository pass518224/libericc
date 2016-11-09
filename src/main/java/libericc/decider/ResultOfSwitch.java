package libericc.decider;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import soot.Unit;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;

/*
 * Represent the result of switch
 */

public class ResultOfSwitch {
	boolean bottom; // whether it is unknown
	Map<Integer, Unit> reachableValueToTarget; // reachable integer value and target pairs
	boolean defaultTargetIsReachable; // whether default target is reachable

	public ResultOfSwitch() {
		bottom = true;
	}
	
	public ResultOfSwitch(Map<Integer, Unit> reachableValueToTarget, boolean defaultTargetIsReachable) {
		bottom = false;
		this.reachableValueToTarget = reachableValueToTarget;
		this.defaultTargetIsReachable = defaultTargetIsReachable;
	}

	// rebuild from json object
	public ResultOfSwitch(SwitchStmt stmt, JSONObject obj) {
		bottom = obj.getBoolean("bottom");
		defaultTargetIsReachable = obj.getBoolean("defaultTargetIsReachable");
		reachableValueToTarget = new HashMap<Integer, Unit>();
		JSONArray reachedValues = obj.getJSONArray("reachableValues");
		if (stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt s = (LookupSwitchStmt) stmt;
			// lookupValue -> target index
			Map<Integer, Integer> lookupValueToIndex = new HashMap<Integer, Integer>();
			for (int i=0; i<s.getTargetCount(); i++) {
				lookupValueToIndex.put(s.getLookupValue(i), i);
			}
			
			for (int i=0; i<reachedValues.length(); i++) {
				int value = reachedValues.getInt(i);
				reachableValueToTarget.put(value, s.getTargets().get(lookupValueToIndex.get(value)));
			}
		}
		else if (stmt instanceof TableSwitchStmt) {
			TableSwitchStmt s = (TableSwitchStmt) stmt;
			int low = s.getLowIndex();
			
			for (int i=0; i<reachedValues.length(); i++) {
				int value = reachedValues.getInt(i);
				reachableValueToTarget.put(value, s.getTargets().get(value-low));
			}
		}
	}
	
	public boolean isBottom() { return bottom; }
	public Map<Integer, Unit> getReachableValueToTarget() { return reachableValueToTarget; }
	public boolean getDefaultTargetIsReachable() { return defaultTargetIsReachable; }
	
	// dump to a json object
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		obj.put("bottom", bottom);
		obj.put("defaultTargetIsReachable", defaultTargetIsReachable);
		JSONArray values = new JSONArray();
		for (Integer value: reachableValueToTarget.keySet()) {
			values.put(value);
		}
		obj.put("reachableValues", values);
		return obj;
	}
	
}
