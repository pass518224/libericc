package hisdroid.edgefunc.bundle;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.BundleValue;
import hisdroid.value.CharValue;

public class BundleGetCharEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	char defaultValue;
	
	public BundleGetCharEdge(){
		this(null);
	}
	
	public BundleGetCharEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetCharEdge(String name, char defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetCharEdge(BundleGetCharEdge old, EdgeFunction<GeneralValue> next){
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
		return new BundleGetCharEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new CharValue();
			
			Set<Character> charSet = new HashSet<Character>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("type").equals("10")) {
						charSet.add(v.getString("_").charAt(0));
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
		if (otherFunction instanceof BundleGetCharEdge) {
			BundleGetCharEdge otherCharEdge = (BundleGetCharEdge) otherFunction;
			if (equals(otherCharEdge)) return this;
			return new BundleGetCharEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetCharEdge) {
			BundleGetCharEdge otherCharEdge = (BundleGetCharEdge) other;
			return name.equals(otherCharEdge.name) && knownDefault == otherCharEdge.knownDefault && defaultValue == otherCharEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetCharEdge()";
		}
		return String.format("BundleGetCharEdge(\"%s\")",name);
	}

}
