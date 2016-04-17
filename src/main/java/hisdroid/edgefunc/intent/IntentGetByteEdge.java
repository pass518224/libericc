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
import hisdroid.value.ByteValue;

public class IntentGetByteEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	byte defaultValue;
	
	public IntentGetByteEdge(){
		this(null);
	}
	
	public IntentGetByteEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetByteEdge(String name, byte defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetByteEdge(IntentGetByteEdge old, EdgeFunction<GeneralValue> next){
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
		return new IntentGetByteEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new ByteValue();
			
			Set<Byte> byteSet = new HashSet<Byte>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("Extras").getJSONObject(name);
					if (v.getString("Type").equals("Byte")) {
						byteSet.add(Byte.valueOf(v.getString("Value")));
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
		if (otherFunction instanceof IntentGetByteEdge) {
			IntentGetByteEdge otherByteEdge = (IntentGetByteEdge) otherFunction;
			if (equals(otherByteEdge)) return this;
			return new IntentGetByteEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetByteEdge) {
			IntentGetByteEdge otherByteEdge = (IntentGetByteEdge) other;
			return name.equals(otherByteEdge.name) && knownDefault == otherByteEdge.knownDefault && defaultValue == otherByteEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetByteEdge()";
		}
		return String.format("IntentGetByteEdge(\"%s\")",name);
	}

}
