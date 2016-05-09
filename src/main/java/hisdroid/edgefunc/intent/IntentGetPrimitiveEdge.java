package hisdroid.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.Utility;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.PrimitiveDataValue;

public abstract class IntentGetPrimitiveEdge<T extends Comparable<T>> extends EdgeFunctionTemplate {
	Class<T> type;
	String name;
	boolean knownDefault;
	T defaultValue;
	
	public IntentGetPrimitiveEdge(Class<T> type){
		this(type, null);
	}
	
	public IntentGetPrimitiveEdge(Class<T> type, String name){
		this.type = type;
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetPrimitiveEdge(Class<T> type, String name, T defaultValue){
		this.type = type;
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	protected IntentGetPrimitiveEdge(IntentGetPrimitiveEdge<T> old, EdgeFunction<GeneralValue> next){
		this.type = old.type;
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public Class<T> type() { return type; }
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public T defaultValue() { return defaultValue; }
	
	abstract T stringToT(String s);

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new PrimitiveDataValue<T>(type);
			Set<T> valueSet = new HashSet<T>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals(String.valueOf(Utility.getParcelTypeNumber(type)))) {
						valueSet.add(stringToT(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added){
					if (knownDefault) valueSet.add(defaultValue);
					else return new PrimitiveDataValue<T>(type);
				}
			}
			return new PrimitiveDataValue<T>(type, valueSet);
		}
		return new PrimitiveDataValue<T>(type);
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetPrimitiveEdge) {
			if (type.equals(((IntentGetPrimitiveEdge<?>)otherFunction).type)) {
				@SuppressWarnings("unchecked")
				IntentGetPrimitiveEdge<T> other = (IntentGetPrimitiveEdge<T>) otherFunction;
				if (equals(other)) return this;
			}
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetPrimitiveEdge) {
			if (type.equals(((IntentGetPrimitiveEdge<?>)other).type)) {
				@SuppressWarnings("unchecked")
				IntentGetPrimitiveEdge<T> otherEdge = (IntentGetPrimitiveEdge<T>) other;
				return name.equals(otherEdge.name) && knownDefault == otherEdge.knownDefault && defaultValue.equals(otherEdge.defaultValue);
			}
		}
		return false;
	}
}
