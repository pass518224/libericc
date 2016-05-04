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
import hisdroid.value.IntValue;
import hisdroid.value.IntentValue;

public class IntentGetIntEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	int defaultValue;
	
	public IntentGetIntEdge(){
		this(null);
	}
	
	public IntentGetIntEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetIntEdge(String name, int defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetIntEdge(IntentGetIntEdge old, EdgeFunction<GeneralValue> next){
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
		return new IntentGetIntEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new IntValue();
			Set<Integer> intSet = new HashSet<Integer>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject(name);
					if (v.getString("type").equals("1")) {
						intSet.add(Integer.valueOf(v.getString("_")));
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
		if (otherFunction instanceof IntentGetIntEdge) {
			IntentGetIntEdge otherIntEdge = (IntentGetIntEdge) otherFunction;
			if (equals(otherIntEdge)) return this;
			return new IntentGetIntEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetIntEdge) {
			IntentGetIntEdge otherIntEdge = (IntentGetIntEdge) other;
			return name.equals(otherIntEdge.name) && knownDefault == otherIntEdge.knownDefault && defaultValue == otherIntEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetIntEdge()";
		}
		return String.format("IntentGetIntEdge(\"%s\")",name);
	}

}
