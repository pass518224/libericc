package libericc.edgefunc.bundle;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

public class BundleGetBooleanEdge extends BundleGetPrimitiveEdge<Integer> {
	public BundleGetBooleanEdge(){
		super(Integer.class);
	}
	
	public BundleGetBooleanEdge(String name){
		super(Integer.class, name);
	}
	
	public BundleGetBooleanEdge(String name, Integer defaultValue){
		super(Integer.class, name, defaultValue);
	}

	BundleGetBooleanEdge(BundleGetBooleanEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetBooleanEdge(this, next);
	}

	Integer stringToT(String s){
		return Integer.parseInt(s);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetBooleanEdge()";
		}
		return String.format("BundleGetBooleanEdge(\"%s\")",name);
	}

}
