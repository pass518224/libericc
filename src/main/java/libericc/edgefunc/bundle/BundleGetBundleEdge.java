package libericc.edgefunc.bundle;

import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.BundleValue;
import libericc.value.GeneralValue;

public class BundleGetBundleEdge extends BundleGetDataEdge<JSONObject> {
	
	public BundleGetBundleEdge(){
		super(JSONObject.class);
	}
	
	public BundleGetBundleEdge(String name){
		super(JSONObject.class, name);
	}

	public BundleGetBundleEdge(BundleGetBundleEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetBundleEdge(this, next);
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
		if (!knownName) return "BundleGetBundleEdge()";
		else return String.format("BundleGetBundleEdge(\"%s\")", name);
	}

}
