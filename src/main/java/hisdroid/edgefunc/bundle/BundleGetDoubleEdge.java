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
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public Double defaultValue() { return defaultValue; }
	
	Double stringToT(String s){
		return Double.parseDouble(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetDoubleEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetDoubleEdge()";
		}
		return String.format("BundleGetDoubleEdge(\"%s\")",name);
	}
}
