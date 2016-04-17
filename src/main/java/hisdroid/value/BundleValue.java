package hisdroid.value;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class BundleValue extends GeneralValue {
	Set<JSONObject> bundles;
	boolean bottom;
	
	public BundleValue() {
		bundles = new HashSet<JSONObject>();
		bottom = true;
	}
	
	public BundleValue(JSONObject bundle) {
		bundles = new HashSet<JSONObject>();
		bundles.add(bundle);
		bottom = false;
	}
	
	public BundleValue(Set<JSONObject> bundles) {
		this.bundles = bundles;
		bottom = false;
	}
	
	public Set<JSONObject> bundles(){ return bundles; }
	public boolean bottom() { return bottom; }
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue) {
		if (otherValue instanceof BundleValue) {
			BundleValue otherBundleValue = (BundleValue) otherValue;
			if (otherBundleValue.bottom || bottom) return new BundleValue();
			Set<JSONObject> newBundles = new HashSet<JSONObject>(bundles);
			newBundles.addAll(otherBundleValue.bundles);
			return new BundleValue(newBundles);
		}
		return BottomValue.v();
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof BundleValue) {
			return ((BundleValue)o).bundles.equals(bundles);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "Bundle";
	}
}
