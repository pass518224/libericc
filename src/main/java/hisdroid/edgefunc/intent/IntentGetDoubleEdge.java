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
import hisdroid.value.DoubleValue;

public class IntentGetDoubleEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	double defaultValue;
	
	public IntentGetDoubleEdge(){
		this(null);
	}
	
	public IntentGetDoubleEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetDoubleEdge(String name, double defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetDoubleEdge(IntentGetDoubleEdge old, EdgeFunction<GeneralValue> next){
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
		return new IntentGetDoubleEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new DoubleValue();
			
			Set<Double> doubleSet = new HashSet<Double>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
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
		if (otherFunction instanceof IntentGetDoubleEdge) {
			IntentGetDoubleEdge otherDoubleEdge = (IntentGetDoubleEdge) otherFunction;
			if (equals(otherDoubleEdge)) return this;
			return new IntentGetDoubleEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetDoubleEdge) {
			IntentGetDoubleEdge otherDoubleEdge = (IntentGetDoubleEdge) other;
			return name.equals(otherDoubleEdge.name) && knownDefault == otherDoubleEdge.knownDefault && defaultValue == otherDoubleEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetDoubleEdge()";
		}
		return String.format("IntentGetDoubleEdge(\"%s\")",name);
	}

}
