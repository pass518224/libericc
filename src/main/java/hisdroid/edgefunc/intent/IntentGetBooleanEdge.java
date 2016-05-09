package hisdroid.edgefunc.intent;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class IntentGetBooleanEdge extends IntentGetPrimitiveEdge<Boolean> {
	
	public IntentGetBooleanEdge(){
		super(Boolean.class);
	}
	
	public IntentGetBooleanEdge(String name){
		super(Boolean.class, name);
	}
	
	public IntentGetBooleanEdge(String name, Boolean defaultValue){
		super(Boolean.class, name, defaultValue);
	}

	IntentGetBooleanEdge(IntentGetBooleanEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Boolean stringToT(String s) {
		return Boolean.parseBoolean(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetBooleanEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "IntentGetBooleanEdge()";
		}
		return String.format("IntentGetBooleanEdge(\"%s\")",name);
	}
}
