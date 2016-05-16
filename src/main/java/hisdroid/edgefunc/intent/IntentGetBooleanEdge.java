package hisdroid.edgefunc.intent;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class IntentGetBooleanEdge extends IntentGetPrimitiveEdge<Integer> {
	
	public IntentGetBooleanEdge(){
		super(Integer.class);
	}
	
	public IntentGetBooleanEdge(String name){
		super(Integer.class, name);
	}
	
	public IntentGetBooleanEdge(String name, Integer defaultValue){
		super(Integer.class, name, defaultValue);
	}

	IntentGetBooleanEdge(IntentGetBooleanEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Integer stringToT(String s) {
		return Integer.parseInt(s);
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
