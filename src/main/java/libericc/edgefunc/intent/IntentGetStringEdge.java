package libericc.edgefunc.intent;

import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;
import libericc.value.StringValue;

public class IntentGetStringEdge extends IntentGetDataEdge<String> {
	String name;
	
	public IntentGetStringEdge(){
		super(String.class);
	}
	
	public IntentGetStringEdge(String name){
		super(String.class, name);
	}

	public IntentGetStringEdge(IntentGetStringEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetStringEdge(this, next);
	}

	@Override
	GeneralValue unknownGeneralValue() {
		return new StringValue();
	}
	
	@Override
	GeneralValue knownGeneralValue(Set<String> vset) {
		return new StringValue(vset);
	}

	@Override
	String parcelTypeNumber() {
		return "0";
	}

	@Override
	String mappedValueToT(JSONObject v) {
		return v.getString("_");
	}

	@Override
	public String edgeToString() {
		if (!knownName) return "IntentGetStringEdge()";
		else return String.format("IntentGetStringEdge(\"%s\")", name);
	}

}
