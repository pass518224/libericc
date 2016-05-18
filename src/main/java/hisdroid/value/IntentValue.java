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

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		GeneralValue tmp = super.joinWith(otherValue);
		if (otherValue instanceof IntentValue && tmp instanceof DataValue) {
			if (type.equals(((DataValue<?>) tmp).type)) {
				@SuppressWarnings("unchecked")
				DataValue<JSONObject> tmpdv = (DataValue<JSONObject>) tmp;
				if (tmpdv.bottom) return new IntentValue();
				else return new IntentValue(tmpdv.valueSet);
			}
		}
		return tmp;
	}

	@Override
	public String toString(){
		if (bottom) return "Unknown Intent";
		else return "Intent: "+valueSet;
	}
	
}
