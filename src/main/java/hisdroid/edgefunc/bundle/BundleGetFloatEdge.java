package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetFloatEdge extends BundleGetPrimitiveEdge<Float> {

	public BundleGetFloatEdge(){
		super(Float.class);
	}
	
	public BundleGetFloatEdge(String name){
		super(Float.class, name);
	}
	
	public BundleGetFloatEdge(String name, Float defaultValue){
		super(Float.class, name, defaultValue);
	}

	BundleGetFloatEdge(BundleGetFloatEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public Float defaultValue() { return defaultValue; }
	
	Float stringToT(String s){
		return Float.parseFloat(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetFloatEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetFloatEdge()";
		}
		return String.format("BundleGetFloatEdge(\"%s\")",name);
	}
}
