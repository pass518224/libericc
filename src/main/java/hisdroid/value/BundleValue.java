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
	public String toString(){
		if (bottom) return "Unknown Bundle";
		else return "Bundle";
	}
}
