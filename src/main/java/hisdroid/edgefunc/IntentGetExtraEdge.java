package hisdroid.edgefunc;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.value.BottomValue;
import hisdroid.value.BundleValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;

public class IntentGetExtraEdge extends EdgeFunctionTemplate {
	@Override
	public EdgeFunctionTemplate copy() {
		return this;
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue) {
			IntentValue intentSource = (IntentValue) source;
			Set<JSONObject> bundleSet = new HashSet<JSONObject>();
			for (JSONObject i: intentSource.intents()) {
				bundleSet.add(i.getJSONObject("Extras"));
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
		return "GetExtraEdge";
	}

}
