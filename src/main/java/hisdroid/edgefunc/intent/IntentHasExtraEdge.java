package hisdroid.edgefunc.intent;


import org.json.JSONException;
import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.edgefunc.AllBottom;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.BooleanValue;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;

public class IntentHasExtraEdge extends EdgeFunctionTemplate {
	String name;
	
	public IntentHasExtraEdge(){
		this.name = null;
	}
	
	public IntentHasExtraEdge(String name){
		this.name = name;
	}

	public IntentHasExtraEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentHasExtraEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue && name != null) {
			IntentValue intentSource = (IntentValue) source;
			if (intentSource.bottom()) return new BooleanValue();
			
			boolean t = false, f = false;  
			for (JSONObject i: intentSource.intents()) {
				try {
					if (i.getJSONObject("Extras").has(name)) t = true;
					else f = true;
				} catch (JSONException e) {
					return new BooleanValue();
				}
			}
			if (t != f) return new BooleanValue(t);
		}
		return new BooleanValue();
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
		if (name == null) return "IntentHasExtraEdge()";
		else return String.format("IntenthasExtraEdge(\"%s\")", name);
	}

}
