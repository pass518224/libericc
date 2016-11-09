package libericc.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.AllBottom;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.BottomValue;
import libericc.value.GeneralValue;
import libericc.value.IntentValue;

public abstract class IntentGetDataEdge<T> extends EdgeFunctionTemplate {
	Class<T> type;
	boolean knownName;
	String name;
	boolean knownDefault;
	T defaultValue;
	
	public IntentGetDataEdge(Class<T> type){
		this.type = type;
		knownName = false;
		knownDefault = false;
	}
	
	public IntentGetDataEdge(Class<T> type, String name){
		this.type = type;
		knownName = true;
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetDataEdge(Class<T> type, String name, T defaultValue){
		this.type = type;
		knownName = true;
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	protected IntentGetDataEdge(IntentGetDataEdge<T> old, EdgeFunction<GeneralValue> next){
		this.type = old.type;
		this.knownName = old.knownName;
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
		if (source instanceof IntentValue && knownName) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return unknownGeneralValue();
			Set<T> valueSet = new HashSet<T>();
			for (JSONObject b: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
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
		if (otherFunction instanceof IntentGetDataEdge) {
			if (type.equals(((IntentGetDataEdge<?>)otherFunction).type)) {
				@SuppressWarnings("unchecked")
				IntentGetDataEdge<T> other = (IntentGetDataEdge<T>) otherFunction;
				if (equals(other)) return this;
			}
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetDataEdge) {
			if (type.equals(((IntentGetDataEdge<?>)other).type)) {
				@SuppressWarnings("unchecked")
				IntentGetDataEdge<T> otherEdge = (IntentGetDataEdge<T>) other;
				return (!knownName && !otherEdge.knownName || knownName && otherEdge.knownName && name.equals(otherEdge.name)) && 
						(!knownDefault && !otherEdge.knownDefault || knownDefault && otherEdge.knownDefault && defaultValue.equals(otherEdge.defaultValue));
			}
		}
		return false;
	}
}
