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
import hisdroid.value.BundleValue;
import hisdroid.value.StringValue;

public class BundleGetStringEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	String defaultValue;
	
	public BundleGetStringEdge(){
		this(null);
	}
	
	public BundleGetStringEdge(String name){
		this.name = name;
		knownDefault = false;
	}

	public BundleGetStringEdge(String name, String defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}
	
	public BundleGetStringEdge(BundleGetStringEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public String defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetStringEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new StringValue();
			Set<String> stringSet = new HashSet<String>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("Type").equals("String")) {
						stringSet.add(v.getString("Value"));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) return new StringValue(defaultValue);
					else return new StringValue();
				}
			}
			return new StringValue(stringSet);
		}
		return new StringValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetStringEdge) {
			BundleGetStringEdge otherStringEdge = (BundleGetStringEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new BundleGetStringEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetStringEdge) {
			BundleGetStringEdge otherStringEdge = (BundleGetStringEdge) other;
			return name.equals(otherStringEdge.name) && knownDefault == otherStringEdge.knownDefault && defaultValue.equals(otherStringEdge.defaultValue);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) return "BundleGetStringEdge()";
		else return String.format("BundleGetStringEdge(\"%s\")", name);
	}

}
