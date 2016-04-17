package hisdroid.value;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class IntentValue extends GeneralValue {
	Set<JSONObject> intents;
	boolean bottom;
	
	public IntentValue() {
		intents = new HashSet<JSONObject>();
		bottom = true;
	}
	
	public IntentValue(JSONObject intent) {
		intents = new HashSet<JSONObject>();
		intents.add(intent);
		bottom = false;
	}
	
	public IntentValue(Set<JSONObject> intents) {
		this.intents = intents;
		bottom = false;
	}
	
	public Set<JSONObject> intents() { return intents; }
	public boolean bottom() { return bottom; }
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue) {
		if (otherValue instanceof IntentValue) {
			IntentValue otherIntentValue = (IntentValue) otherValue;
			if (otherIntentValue.bottom || bottom) return new IntentValue();
			Set<JSONObject> newIntents = new HashSet<JSONObject>(intents);
			newIntents.addAll(otherIntentValue.intents);
			return new IntentValue(newIntents);
		}
		return BottomValue.v();
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof IntentValue) {
			return ((IntentValue)o).intents.equals(intents);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "Intent: "+intents;
	}
	
}
