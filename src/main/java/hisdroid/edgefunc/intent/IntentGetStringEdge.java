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
import hisdroid.value.StringValue;

public class IntentGetStringEdge extends EdgeFunctionTemplate {
	String name;
	
	public IntentGetStringEdge(){
		this.name = null;
	}
	
	public IntentGetStringEdge(String name){
		this.name = name;
	}

	public IntentGetStringEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetStringEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new StringValue();
			Set<String> stringSet = new HashSet<String>();
			for (JSONObject i: intentSource.intents()) {
				boolean added = false;
				try {
					JSONObject v = i.getJSONObject("mExtras").getJSONObject("map").getJSONObject(name);
					if (v.getString("type").equals("0")) {
						stringSet.add(v.getString("_"));
						added = true;
					}
				} catch (JSONException e) {}
				if (!added) return new StringValue();
			}
			return new StringValue(stringSet);
		}
		return new StringValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetStringEdge) {
			IntentGetStringEdge otherStringEdge = (IntentGetStringEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new IntentGetStringEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetStringEdge) {
			IntentGetStringEdge otherStringEdge = (IntentGetStringEdge) other;
			return name.equals(otherStringEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (name == null) return "IntentGetStringEdge()";
		else return String.format("IntentGetStringEdge(\"%s\")", name);
	}

}
