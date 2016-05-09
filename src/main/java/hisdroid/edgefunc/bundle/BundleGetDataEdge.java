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

public abstract class BundleGetDataEdge<T> extends EdgeFunctionTemplate {
	Class<T> type;
	boolean knownName;
	String name;
	boolean knownDefault;
	T defaultValue;
	
	public BundleGetDataEdge(Class<T> type){
		this.type = type;
		knownName = false;
		knownDefault = false;
	}
	
	public BundleGetDataEdge(Class<T> type, String name){
		this.type = type;
		knownName = true;
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetDataEdge(Class<T> type, String name, T defaultValue){
		this.type = type;
		knownName = true;
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	protected BundleGetDataEdge(BundleGetDataEdge<T> old, EdgeFunction<GeneralValue> next){
		this.type = old.type;
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public Class<T> type() { return type; }
	public boolean knownName() { return knownName; }
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public T defaultValue() { return defaultValue; }

	abstract GeneralValue unknownGeneralValue();
	abstract GeneralValue knownGeneralValue(Set<T> vset);
	abstract String parcelTypeNumber();
	abstract T mappedValueToT(JSONObject v);
	
	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return unknownGeneralValue();
			Set<T> valueSet = new HashSet<T>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals(parcelTypeNumber())) {
						valueSet.add(mappedValueToT(v));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added){
					if (knownDefault) valueSet.add(defaultValue);
					else return unknownGeneralValue();
				}
			}
			return knownGeneralValue(valueSet);
		}
		return unknownGeneralValue();
	}
	
	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetDataEdge) {
			if (type.equals(((BundleGetDataEdge<?>)otherFunction).type)) {
				@SuppressWarnings("unchecked")
				BundleGetDataEdge<T> other = (BundleGetDataEdge<T>) otherFunction;
				if (equals(other)) return this;
			}
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetDataEdge) {
			if (type.equals(((BundleGetDataEdge<?>)other).type)) {
				@SuppressWarnings("unchecked")
				BundleGetDataEdge<T> otherEdge = (BundleGetDataEdge<T>) other;
				return (!knownName && !otherEdge.knownName || knownName && otherEdge.knownName && name.equals(otherEdge.name)) && 
						(!knownDefault && !otherEdge.knownDefault || knownDefault && otherEdge.knownDefault && defaultValue.equals(otherEdge.defaultValue));
			}
		}
		return false;
	}
}
