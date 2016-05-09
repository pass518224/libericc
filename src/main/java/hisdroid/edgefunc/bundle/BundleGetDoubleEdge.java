package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetDoubleEdge extends BundleGetPrimitiveEdge<Double> {

	public BundleGetDoubleEdge(){
		super(Double.class);
	}
	
	public BundleGetDoubleEdge(String name){
		super(Double.class, name);
	}
	
	public BundleGetDoubleEdge(String name, Double defaultValue){
		super(Double.class, name, defaultValue);
	}

	BundleGetDoubleEdge(BundleGetDoubleEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	Double stringToT(String s){
		return Double.parseDouble(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetDoubleEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetDoubleEdge()";
		}
		return String.format("BundleGetDoubleEdge(\"%s\")",name);
	}
}
