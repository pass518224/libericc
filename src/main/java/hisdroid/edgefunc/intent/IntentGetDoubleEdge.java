package hisdroid.edgefunc.intent;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class IntentGetDoubleEdge extends IntentGetPrimitiveEdge<Double> {
	
	public IntentGetDoubleEdge(){
		super(Double.class);
	}
	
	public IntentGetDoubleEdge(String name){
		super(Double.class, name);
	}
	
	public IntentGetDoubleEdge(String name, Double defaultValue){
		super(Double.class, name, defaultValue);
	}

	IntentGetDoubleEdge(IntentGetDoubleEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Double stringToT(String s) {
		return Double.parseDouble(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetDoubleEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "IntentGetDoubleEdge()";
		}
		return String.format("IntentGetDoubleEdge(\"%s\")",name);
	}
}
