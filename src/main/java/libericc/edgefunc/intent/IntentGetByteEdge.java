package libericc.edgefunc.intent;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

public class IntentGetByteEdge extends IntentGetPrimitiveEdge<Byte> {
	
	public IntentGetByteEdge(){
		super(Byte.class);
	}
	
	public IntentGetByteEdge(String name){
		super(Byte.class, name);
	}
	
	public IntentGetByteEdge(String name, Byte defaultValue){
		super(Byte.class, name, defaultValue);
	}

	IntentGetByteEdge(IntentGetByteEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}

	@Override
	Byte stringToT(String s) {
		return Byte.parseByte(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetByteEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "IntentGetByteEdge()";
		}
		return String.format("IntentGetByteEdge(\"%s\")",name);
	}
}
