package hisdroid.edgefunc.intent;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class IntentGetFloatEdge extends IntentGetPrimitiveEdge<Float> {
	
	public IntentGetFloatEdge(){
		super(Float.class);
	}
	
	public IntentGetFloatEdge(String name){
		super(Float.class, name);
	}
	
	public IntentGetFloatEdge(String name, Float defaultValue){
		super(Float.class, name, defaultValue);
	}

	IntentGetFloatEdge(IntentGetFloatEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Float stringToT(String s) {
		return Float.parseFloat(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetFloatEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetFloatEdge()";
		}
		return String.format("IntentGetFloatEdge(\"%s\")",name);
	}
}
