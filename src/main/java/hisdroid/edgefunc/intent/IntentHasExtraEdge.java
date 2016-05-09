package hisdroid.edgefunc.intent;


import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.PrimitiveDataValue;

public class IntentHasExtraEdge extends EdgeFunctionTemplate {
	boolean knownName;
	String name;
	
	public IntentHasExtraEdge(){
		knownName = false;
	}
	
	public IntentHasExtraEdge(String name){
		knownName = true;
		this.name = name;
	}

	public IntentHasExtraEdge(IntentHasExtraEdge old, EdgeFunction<GeneralValue> next){
		knownName = old.knownName;
		name = old.name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentHasExtraEdge(this, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new PrimitiveDataValue<Boolean>(Boolean.class);
			
			boolean t = false, f = false;  
			for (JSONObject i: intentSource.intents()) {
				try {
					if (i.getJSONObject("mExtras").getJSONObject("map").has(name)) t = true;
					else f = true;
				} catch (JSONException e) {
					return new PrimitiveDataValue<Boolean>(Boolean.class);
				}
			}
			if (t != f) return new PrimitiveDataValue<Boolean>(Boolean.class, t);
		}
		return new PrimitiveDataValue<Boolean>(Boolean.class);
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentHasExtraEdge) {
			IntentHasExtraEdge otherStringEdge = (IntentHasExtraEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new IntentHasExtraEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentHasExtraEdge) {
			IntentHasExtraEdge otherBooleanEdge = (IntentHasExtraEdge) other;
			return name.equals(otherBooleanEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		if (!knownName) return "IntentHasExtraEdge()";
		else return String.format("IntenthasExtraEdge(\"%s\")", name);
	}

}
