package hisdroid.value;

import java.util.Set;

import org.json.JSONObject;

public class IntentValue extends DataValue<JSONObject> {
	
	public IntentValue() {
		super(JSONObject.class);
	}
	
	public IntentValue(JSONObject intent) {
		super(JSONObject.class, intent);
	}
	
	public IntentValue(Set<JSONObject> intents) {
		super(JSONObject.class, intents);
	}
	
	public Set<JSONObject> intents() { return valueSet(); }
	public boolean bottom() { return bottom; }

	@Override
	public String toString(){
		if (bottom) return "Unknown Intent";
		else return "Intent";
	}
	
}
