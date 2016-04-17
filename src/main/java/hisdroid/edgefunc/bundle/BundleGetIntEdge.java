package hisdroid.edgefunc.bundle;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValue;
import hisdroid.value.BundleValue;

public class BundleGetIntEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	int defaultValue;
	
	public BundleGetIntEdge(){
		this(null);
	}
	
	public BundleGetIntEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetIntEdge(String name, int defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetIntEdge(BundleGetIntEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public int defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetIntEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new IntValue();
			Set<Integer> intSet = new HashSet<Integer>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("Type").equals("Int")) {
						intSet.add(Integer.valueOf(v.getString("Value")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added){
					if (knownDefault) intSet.add(defaultValue);
					else return new IntValue();
				}
			}
			return new IntValue(intSet);
		}
		return new IntValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetIntEdge) {
			BundleGetIntEdge otherIntEdge = (BundleGetIntEdge) otherFunction;
			if (equals(otherIntEdge)) return this;
			return new BundleGetIntEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetIntEdge) {
			BundleGetIntEdge otherIntEdge = (BundleGetIntEdge) other;
			return name.equals(otherIntEdge.name) && knownDefault == otherIntEdge.knownDefault && defaultValue == otherIntEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetIntEdge()";
		}
		return String.format("BundleGetIntEdge(\"%s\")",name);
	}

}
