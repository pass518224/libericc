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
import hisdroid.value.CharValue;

public class IntentGetCharEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	char defaultValue;
	
	public IntentGetCharEdge(){
		this(null);
	}
	
	public IntentGetCharEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetCharEdge(String name, char defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetCharEdge(IntentGetCharEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public char defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetCharEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new CharValue();
			
			Set<Character> charSet = new HashSet<Character>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("Extras").getJSONObject(name);
					if (v.getString("Type").equals("Char")) {
						charSet.add(v.getString("Value").charAt(0));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) {
						charSet.add(defaultValue);
					} 
					else {
						return new CharValue();
					}
				}
			}
			return new CharValue(charSet);
		}
		return new CharValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetCharEdge) {
			IntentGetCharEdge otherCharEdge = (IntentGetCharEdge) otherFunction;
			if (equals(otherCharEdge)) return this;
			return new IntentGetCharEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetCharEdge) {
			IntentGetCharEdge otherCharEdge = (IntentGetCharEdge) other;
			return name.equals(otherCharEdge.name) && knownDefault == otherCharEdge.knownDefault && defaultValue == otherCharEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetCharEdge()";
		}
		return String.format("IntentGetCharEdge(\"%s\")",name);
	}

}
