package hisdroid.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.StringValue;

public class IntentGetActionEdge extends EdgeFunctionTemplate {
	@Override
	public EdgeFunctionTemplate copy() {
		return this;
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new StringValue();
			Set<String> bundleSet = new HashSet<String>();
			for (JSONObject i: intentSource.intents()) {
				try {
					bundleSet.add(i.getString("Actions"));
				}
				catch (JSONException e) {
					return new StringValue();
				}
			}
			return new StringValue(bundleSet);
		}
		return new StringValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetActionEdge) {
			return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetActionEdge) {
			return true;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		return "IntentGetActionEdge";
	}

}
