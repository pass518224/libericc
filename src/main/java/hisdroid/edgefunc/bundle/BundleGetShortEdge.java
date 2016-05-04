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
import hisdroid.value.ShortValue;

public class BundleGetShortEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	short defaultValue;
	
	public BundleGetShortEdge(){
		this(null);
	}
	
	public BundleGetShortEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetShortEdge(String name, short defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetShortEdge(BundleGetShortEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public short defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetShortEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new ShortValue();
			Set<Short> shortSet = new HashSet<Short>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("type").equals("5")) {
						shortSet.add(Short.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) shortSet.add(defaultValue);
					else return new ShortValue();
				}
			}
			return new ShortValue(shortSet);
		}
		return new ShortValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetShortEdge) {
			BundleGetShortEdge otherShortEdge = (BundleGetShortEdge) otherFunction;
			if (equals(otherShortEdge)) return this;
			return new BundleGetShortEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetShortEdge) {
			BundleGetShortEdge otherShortEdge = (BundleGetShortEdge) other;
			return name.equals(otherShortEdge.name) && knownDefault == otherShortEdge.knownDefault && defaultValue == otherShortEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetShortEdge()";
		}
		return String.format("BundleGetShortEdge(\"%s\")",name);
	}

}
