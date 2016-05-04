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
import hisdroid.value.ShortValue;

public class IntentGetShortEdge extends EdgeFunctionTemplate {
	String name;
	boolean knownDefault;
	short defaultValue;
	
	public IntentGetShortEdge(){
		this(null);
	}
	
	public IntentGetShortEdge(String name){
		this.name = name;
		knownDefault = false;
	}
	
	public IntentGetShortEdge(String name, short defaultValue){
		this.name = name;
		knownDefault = true;
		this.defaultValue = defaultValue;
	}

	IntentGetShortEdge(IntentGetShortEdge old, EdgeFunction<GeneralValue> next){
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
		return new IntentGetShortEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new ShortValue();
			Set<Short> shortSet = new HashSet<Short>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject(name);
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
		if (otherFunction instanceof IntentGetShortEdge) {
			IntentGetShortEdge otherShortEdge = (IntentGetShortEdge) otherFunction;
			if (equals(otherShortEdge)) return this;
			return new IntentGetShortEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetShortEdge) {
			IntentGetShortEdge otherShortEdge = (IntentGetShortEdge) other;
			return name.equals(otherShortEdge.name) && knownDefault == otherShortEdge.knownDefault && defaultValue == otherShortEdge.defaultValue;
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetShortEdge()";
		}
		return String.format("IntentGetShortEdge(\"%s\")",name);
	}

}
