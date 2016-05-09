package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetLongEdge extends BundleGetPrimitiveEdge<Long> {

	public BundleGetLongEdge(){
		super(Long.class);
	}
	
	public BundleGetLongEdge(String name){
		super(Long.class, name);
	}
	
	public BundleGetLongEdge(String name, Long defaultValue){
		super(Long.class, name, defaultValue);
	}

	BundleGetLongEdge(BundleGetLongEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	public String name() { return name; }
	public boolean knownDefault() { return knownDefault; }
	public Long defaultValue() { return defaultValue; }
	
	Long stringToT(String s){
		return Long.parseLong(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetLongEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (name == null) {
			return "BundleGetLongEdge()";
		}
		return String.format("BundleGetLongEdge(\"%s\")",name);
	}
}
