package hisdroid.edgefunc.bundle;

import heros.EdgeFunction;
import hisdroid.edgefunc.EdgeFunctionTemplate;
import hisdroid.value.GeneralValue;

public class BundleGetShortEdge extends BundleGetPrimitiveEdge<Short> {

	public BundleGetShortEdge(){
		super(Short.class);
	}
	
	public BundleGetShortEdge(String name){
		super(Short.class, name);
	}
	
	public BundleGetShortEdge(String name, Short defaultValue){
		super(Short.class, name, defaultValue);
	}

	BundleGetShortEdge(BundleGetShortEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	Short stringToT(String s){
		return Short.parseShort(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetShortEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetShortEdge()";
		}
		return String.format("BundleGetShortEdge(\"%s\")",name);
	}
}
