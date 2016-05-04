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
import hisdroid.value.ByteValue;

public class BundleGetByteEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	byte defaultValue;
	
	public BundleGetByteEdge(){
		this(null);
	}
	
	public BundleGetByteEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public BundleGetByteEdge(String name, byte defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	BundleGetByteEdge(BundleGetByteEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public byte defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetByteEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof BundleValue && name != null) {
			BundleValue bundleSource = (BundleValue) source;
			if (bundleSource.bottom()) return new ByteValue();
			
			Set<Byte> byteSet = new HashSet<Byte>();
			for (JSONObject b: bundleSource.bundles()) {
				boolean added = false;
				try {
					JSONObject v = b.getJSONObject(name);
					if (v.getString("type").equals("20")) {
						byteSet.add(Byte.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) {
					if (knownDefault) {
						byteSet.add(defaultValue);
					} 
					else {
						return new ByteValue();
					}
				}
			}
			return new ByteValue(byteSet);
		}
		return new ByteValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof BundleGetByteEdge) {
			BundleGetByteEdge otherByteEdge = (BundleGetByteEdge) otherFunction;
			if (equals(otherByteEdge)) return this;
			return new BundleGetByteEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof BundleGetByteEdge) {
			BundleGetByteEdge otherByteEdge = (BundleGetByteEdge) other;
			return name.equals(otherByteEdge.name) && knownDefault == otherByteEdge.knownDefault && defaultValue == otherByteEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetByteEdge()";
		}
		return String.format("BundleGetByteEdge(\"%s\")",name);
	}

}
