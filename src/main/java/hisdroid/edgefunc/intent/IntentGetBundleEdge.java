package hisdroid.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.BundleValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;

public class IntentGetBundleEdge extends EdgeFunctionTemplate {
	String name;
	
	public IntentGetBundleEdge(){
		this.name = null;
	}
	
	public IntentGetBundleEdge(String name){
		this.name = name;
	}

	public IntentGetBundleEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetBundleEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new BundleValue();
			
			Set<JSONObject> bundleSet = new HashSet<JSONObject>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals("3")) {
						bundleSet.add(v.getJSONObject("_"));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					return new BundleValue();
				}
			}
			return new BundleValue(bundleSet);
		}
		return new BundleValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetBundleEdge) {
			IntentGetBundleEdge otherStringEdge = (IntentGetBundleEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new IntentGetBundleEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetBundleEdge) {
			IntentGetBundleEdge otherBundleEdge = (IntentGetBundleEdge) other;
			return name.equals(otherBundleEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) return "IntentGetBundleEdge()";
		else return String.format("IntentGetBundleEdge(\"%s\")", name);
	}

}
