package libericc.decider;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import libericc.PredictResult;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public class RandomDecider implements Decider {
	
	Map<IfStmt, PredictResult> m = new HashMap<IfStmt, PredictResult>();

	@Override
	public PredictResult branchPredictResult(IfStmt stmt) {
		if (m.containsKey(stmt)) {
			return m.get(stmt);
		}
		else {
			Random random = new Random();
			PredictResult result;
			double r = random.nextDouble()*3;
			if (r < 1) {
				result = PredictResult.True;
			}
			else if (r < 2) {
				result = PredictResult.False;
			}
			else {
				result = PredictResult.Both;
			}
			m.put(stmt, result);
			return result;
		}
	}

	@Override
	public ResultOfSwitch switchResult(SwitchStmt stmt) {
		return new ResultOfSwitch();
	}

}
