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
import hisdroid.value.LongValue;

public class IntentGetLongEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	long defaultValue;
	
	public IntentGetLongEdge(){
		this(null);
	}
	
	public IntentGetLongEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetLongEdge(String name, long defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetLongEdge(IntentGetLongEdge old, EdgeFunction<GeneralValue> next){
		this.name = old.name;
		this.knownDefault = old.knownDefault;
		this.defaultValue = old.defaultValue;
		this.next = next;
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public long defaultValue() { return defaultValue; }
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetLongEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new LongValue();
			Set<Long> longSet = new HashSet<Long>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject(name);
					if (v.getString("type").equals("6")) {
						longSet.add(Long.valueOf(v.getString("_")));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added){
					if (knownDefault) longSet.add(defaultValue);
					else new LongValue();
				}
			}
			return new LongValue(longSet);
		}
		return new LongValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetLongEdge) {
			IntentGetLongEdge otherLongEdge = (IntentGetLongEdge) otherFunction;
			if (equals(otherLongEdge)) return this;
			return new IntentGetLongEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetLongEdge) {
			IntentGetLongEdge otherLongEdge = (IntentGetLongEdge) other;
			return name.equals(otherLongEdge.name) && knownDefault == otherLongEdge.knownDefault && defaultValue == otherLongEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetLongEdge()";
		}
		return String.format("IntentGetLongEdge(\"%s\")",name);
	}

}
