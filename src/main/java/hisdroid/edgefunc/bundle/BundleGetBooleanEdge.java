package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetBooleanEdge extends BundleGetPrimitiveEdge<Boolean> {
	public BundleGetBooleanEdge(){
		super(Boolean.class);
	}
	
	public BundleGetBooleanEdge(String name){
		super(Boolean.class, name);
	}
	
	public BundleGetBooleanEdge(String name, Boolean defaultValue){
		super(Boolean.class, name, defaultValue);
	}

	BundleGetBooleanEdge(BundleGetBooleanEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetBooleanEdge(this, next);
	}

	Boolean stringToT(String s){
		return Boolean.parseBoolean(s);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetBooleanEdge()";
		}
		return String.format("BundleGetBooleanEdge(\"%s\")",name);
	}

}
