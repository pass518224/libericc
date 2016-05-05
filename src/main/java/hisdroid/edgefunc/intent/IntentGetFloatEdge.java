package hisdroid.edgefunc.intent;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.FloatValue;

public class IntentGetFloatEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	float defaultValue;
	
	public IntentGetFloatEdge(){
		this(null);
	}
	
	public IntentGetFloatEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetFloatEdge(String name, float defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetFloatEdge(IntentGetFloatEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public float defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetFloatEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new FloatValue();
			Set<Float> floatSet = new HashSet<Float>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals("7")) {
						floatSet.add(Float.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) floatSet.add(defaultValue);
					else return new FloatValue();
				}
			}
			return new FloatValue(floatSet);
		}
		return new FloatValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetFloatEdge) {
			IntentGetFloatEdge otherFloatEdge = (IntentGetFloatEdge) otherFunction;
			if (equals(otherFloatEdge)) return this;
			return new IntentGetFloatEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetFloatEdge) {
			IntentGetFloatEdge otherFloatEdge = (IntentGetFloatEdge) other;
			return name.equals(otherFloatEdge.name) && knownDefault == otherFloatEdge.knownDefault && defaultValue == otherFloatEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetFloatEdge()";
		}
		return String.format("IntentGetFloatEdge(\"%s\")",name);
	}

}
