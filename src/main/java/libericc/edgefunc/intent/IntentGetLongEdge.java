package libericc.edgefunc.intent;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

public class IntentGetLongEdge extends IntentGetPrimitiveEdge<Long> {
	
	public IntentGetLongEdge(){
		super(Long.class);
	}
	
	public IntentGetLongEdge(String name){
		super(Long.class, name);
	}
	
	public IntentGetLongEdge(String name, Long defaultValue){
		super(Long.class, name, defaultValue);
	}

	IntentGetLongEdge(IntentGetLongEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Long stringToT(String s) {
		return Long.parseLong(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetLongEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "IntentGetLongEdge()";
		}
		return String.format("IntentGetLongEdge(\"%s\")",name);
	}
}
