package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetIntEdge extends BundleGetPrimitiveEdge<Integer> {

	public BundleGetIntEdge(){
		super(Integer.class);
	}
	
	public BundleGetIntEdge(String name){
		super(Integer.class, name);
	}
	
	public BundleGetIntEdge(String name, Integer defaultValue){
		super(Integer.class, name, defaultValue);
	}

	BundleGetIntEdge(BundleGetIntEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public Integer defaultValue() { return defaultValue; }
	
	Integer stringToT(String s){
		return Integer.parseInt(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetIntEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetIntEdge()";
		}
		return String.format("BundleGetIntEdge(\"%s\")",name);
	}
}
