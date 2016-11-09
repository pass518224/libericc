package libericc.edgefunc.intent;

import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.BundleValue;
import libericc.value.GeneralValue;

public class IntentGetBundleEdge extends IntentGetDataEdge<JSONObject> {
	String name;
	
	public IntentGetBundleEdge(){
		super(JSONObject.class);
	}
	
	public IntentGetBundleEdge(String name){
		super(JSONObject.class, name);
	}

	public IntentGetBundleEdge(IntentGetBundleEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetBundleEdge(this, next);
	}

	@Override
	GeneralValue unknownGeneralValue() {
		return new BundleValue();
	}
	
	@Override
	GeneralValue knownGeneralValue(Set<JSONObject> vset) {
		return new BundleValue(vset);
	}

	@Override
	String parcelTypeNumber() {
		return "3";
	}

	@Override
	JSONObject mappedValueToT(JSONObject v) {
		return v.getJSONObject("_");
	}

	@Override
	public String edgeToString() {
		if (!knownName) return "IntentGetBundleEdge()";
		else return String.format("IntentGetBundleEdge(\"%s\")", name);
	}

}
