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
import hisdroid.value.DoubleValue;

public class BundleGetDoubleEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	double defaultValue;
	
	public BundleGetDoubleEdge(){
		this(null);
	}
	
	public BundleGetDoubleEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetDoubleEdge(String name, double defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetDoubleEdge(BundleGetDoubleEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public double defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetDoubleEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new DoubleValue();
			
			Set<Double> doubleSet = new HashSet<Double>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("type").equals("8")) {
						doubleSet.add(Double.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) doubleSet.add(defaultValue);
					else return new DoubleValue();
				}
			}
			return new DoubleValue(doubleSet);
		}
		return new DoubleValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetDoubleEdge) {
			BundleGetDoubleEdge otherDoubleEdge = (BundleGetDoubleEdge) otherFunction;
			if (equals(otherDoubleEdge)) return this;
			return new BundleGetDoubleEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetDoubleEdge) {
			BundleGetDoubleEdge otherDoubleEdge = (BundleGetDoubleEdge) other;
			return name.equals(otherDoubleEdge.name) && knownDefault == otherDoubleEdge.knownDefault && defaultValue == otherDoubleEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetDoubleEdge()";
		}
		return String.format("BundleGetDoubleEdge(\"%s\")",name);
	}

}
