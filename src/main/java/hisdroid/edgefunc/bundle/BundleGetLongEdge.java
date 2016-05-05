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
import hisdroid.value.LongValue;

public class BundleGetLongEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	long defaultValue;
	
	public BundleGetLongEdge(){
		this(null);
	}
	
	public BundleGetLongEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetLongEdge(String name, long defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetLongEdge(BundleGetLongEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public long defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetLongEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new LongValue();
			Set<Long> longSet = new HashSet<Long>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals("6")) {
						longSet.add(Long.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added){
					if (knownDefault) longSet.add(defaultValue);
					else new LongValue();
				}
			}
			return new LongValue(longSet);
		}
		return new LongValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetLongEdge) {
			BundleGetLongEdge otherLongEdge = (BundleGetLongEdge) otherFunction;
			if (equals(otherLongEdge)) return this;
			return new BundleGetLongEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetLongEdge) {
			BundleGetLongEdge otherLongEdge = (BundleGetLongEdge) other;
			return name.equals(otherLongEdge.name) && knownDefault == otherLongEdge.knownDefault && defaultValue == otherLongEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetLongEdge()";
		}
		return String.format("BundleGetLongEdge(\"%s\")",name);
	}

}
