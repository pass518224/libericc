package libericc.edgefunc.intent;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

public class IntentGetIntEdge extends IntentGetPrimitiveEdge<Integer> {
	
	public IntentGetIntEdge(){
		super(Integer.class);
	}
	
	public IntentGetIntEdge(String name){
		super(Integer.class, name);
	}
	
	public IntentGetIntEdge(String name, Integer defaultValue){
		super(Integer.class, name, defaultValue);
	}

	IntentGetIntEdge(IntentGetIntEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Integer stringToT(String s) {
		return Integer.parseInt(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetIntEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "IntentGetIntEdge()";
		}
		return String.format("IntentGetIntEdge(\"%s\")",name);
	}
}
