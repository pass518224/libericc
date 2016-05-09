package hisdroid.edgefunc.intent;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class IntentGetShortEdge extends IntentGetPrimitiveEdge<Short> {
	
	public IntentGetShortEdge(){
		super(Short.class);
	}
	
	public IntentGetShortEdge(String name){
		super(Short.class, name);
	}
	
	public IntentGetShortEdge(String name, Short defaultValue){
		super(Short.class, name, defaultValue);
	}

	IntentGetShortEdge(IntentGetShortEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Short stringToT(String s) {
		return Short.parseShort(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetShortEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "IntentGetShortEdge()";
		}
		return String.format("IntentGetShortEdge(\"%s\")",name);
	}
}
