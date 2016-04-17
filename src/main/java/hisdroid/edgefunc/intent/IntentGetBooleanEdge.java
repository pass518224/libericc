package hisdroid.edgefunc.intent;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.BooleanValue;

public class IntentGetBooleanEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	boolean defaultValue;
	
	public IntentGetBooleanEdge(){
		this(null);
	}
	
	public IntentGetBooleanEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetBooleanEdge(String name, boolean defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetBooleanEdge(IntentGetBooleanEdge old, EdgeFunction<GeneralValue> next){
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
		return new IntentGetBooleanEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new BooleanValue();
			
			boolean t = false, f = false;
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("Extras").getJSONObject(name);
					if (v.getString("Type").equals("Boolean")) {
						if (Boolean.valueOf(v.getString("Value"))) t =  true;
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
		if (otherFunction instanceof IntentGetBooleanEdge) {
			IntentGetBooleanEdge otherBooleanEdge = (IntentGetBooleanEdge) otherFunction;
			if (equals(otherBooleanEdge)) return this;
			return new IntentGetBooleanEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetBooleanEdge) {
			IntentGetBooleanEdge otherBooleanEdge = (IntentGetBooleanEdge) other;
			return name.equals(otherBooleanEdge.name) && knownDefault == otherBooleanEdge.knownDefault && defaultValue == otherBooleanEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetBooleanEdge()";
		}
		return String.format("IntentGetBooleanEdge(\"%s\")",name);
	}

}
