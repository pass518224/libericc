package hisdroid.value;

import java.util.Set;

import org.json.JSONObject;

public class BundleValue extends DataValue<JSONObject> {
	
	public BundleValue() {
		super(JSONObject.class);
	}
	
	public BundleValue(JSONObject bundle) {
		super(JSONObject.class, bundle);
	}
	
	public BundleValue(Set<JSONObject> bundles) {
		super(JSONObject.class, bundles);
	}
	
	public Set<JSONObject> bundles() { return valueSet(); }

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		GeneralValue tmp = super.joinWith(otherValue);
		if (otherValue instanceof IntentValue && tmp instanceof DataValue) {
			if (type.equals(((DataValue<?>) tmp).type)) {
				@SuppressWarnings("unchecked")
				DataValue<JSONObject> tmpdv = (DataValue<JSONObject>) tmp;
				if (tmpdv.bottom) return new BundleValue();
				else return new BundleValue(tmpdv.valueSet);
			}
		}
		return tmp;
	}

	@Override
	public String toString(){
		if (bottom) return "Unknown Bundle";
		else return "Bundle: "+valueSet;
	}
}
