package libericc.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.AllBottom;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.BottomValue;
import libericc.value.BundleValue;
import libericc.value.GeneralValue;
import libericc.value.IntentValue;

public class IntentGetExtraEdge extends EdgeFunctionTemplate {
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetExtraEdge();
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new BundleValue();
			
			Set<JSONObject> bundleSet = new HashSet<JSONObject>();
			for (JSONObject i: intentSource.intents()) {
				try {
					bundleSet.add(i.getJSONObject("mExtras"));
				}
				catch (JSONException e) {
					return new BundleValue();
				}
			}
			return new BundleValue(bundleSet);
		}
		return new BundleValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetExtraEdge) {
			return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetExtraEdge) {
			return true;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		return "IntentGetExtraEdge";
	}

}
