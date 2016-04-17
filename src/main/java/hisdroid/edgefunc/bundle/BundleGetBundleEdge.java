package hisdroid.edgefunc.bundle;

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

public class BundleGetBundleEdge extends EdgeFunctionTemplate {
	String name;
	
	public BundleGetBundleEdge(){
		this.name = null;
	}
	
	public BundleGetBundleEdge(String name){
		this.name = name;
	}

	public BundleGetBundleEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetBundleEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new BundleValue();
			
			Set<JSONObject> bundleSet = new HashSet<JSONObject>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("Type").equals("Bundle")) {
						bundleSet.add(v.getJSONObject("Value"));
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
		if (otherFunction instanceof BundleGetBundleEdge) {
			BundleGetBundleEdge otherStringEdge = (BundleGetBundleEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new BundleGetBundleEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetBundleEdge) {
			BundleGetBundleEdge otherBundleEdge = (BundleGetBundleEdge) other;
			return name.equals(otherBundleEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) return "BundleGetBundleEdge()";
		else return String.format("BundleGetBundleEdge(\"%s\")", name);
	}

}
