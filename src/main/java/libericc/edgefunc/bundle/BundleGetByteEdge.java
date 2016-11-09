package libericc.edgefunc.bundle;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

public class BundleGetByteEdge extends BundleGetPrimitiveEdge<Byte> {

	public BundleGetByteEdge(){
		super(Byte.class);
	}
	
	public BundleGetByteEdge(String name){
		super(Byte.class, name);
	}
	
	public BundleGetByteEdge(String name, Byte defaultValue){
		super(Byte.class, name, defaultValue);
	}

	BundleGetByteEdge(BundleGetByteEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	Byte stringToT(String s){
		return Byte.parseByte(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetByteEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetByteEdge()";
		}
		return String.format("BundleGetByteEdge(\"%s\")",name);
	}
}
