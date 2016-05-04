package hisdroid.edgefunc.bundle;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.BundleValue;
import hisdroid.value.BooleanValue;

public class BundleGetBooleanEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	boolean defaultValue;
	
	public BundleGetBooleanEdge(){
		this(null);
	}
	
	public BundleGetBooleanEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetBooleanEdge(String name, boolean defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetBooleanEdge(BundleGetBooleanEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public boolean defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetBooleanEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new BooleanValue();
			
			boolean t = false, f = false;
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("type").equals("9")) {
						if (Boolean.valueOf(v.getString("_"))) t =  true;
						else f = true;
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) {
						if (defaultValue) t = true;
						else f = true;
					} 
					else {
						return new BooleanValue();
					}
				}
				if (t && f) return new BooleanValue();
			}
			if (t != f) return new BooleanValue(t);
		}
		return new BooleanValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetBooleanEdge) {
			BundleGetBooleanEdge otherBooleanEdge = (BundleGetBooleanEdge) otherFunction;
			if (equals(otherBooleanEdge)) return this;
			return new BundleGetBooleanEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetBooleanEdge) {
			BundleGetBooleanEdge otherBooleanEdge = (BundleGetBooleanEdge) other;
			return name.equals(otherBooleanEdge.name) && knownDefault == otherBooleanEdge.knownDefault && defaultValue == otherBooleanEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetBooleanEdge()";
		}
		return String.format("BundleGetBooleanEdge(\"%s\")",name);
	}

}
