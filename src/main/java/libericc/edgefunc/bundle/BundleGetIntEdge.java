package libericc.edgefunc.bundle;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;

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
	
	Integer stringToT(String s){
		return Integer.parseInt(s);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetIntEdge(this, next);
	}

	@Override
	public String edgeToString() {
		if (!knownName) {
			return "BundleGetIntEdge()";
		}
		return String.format("BundleGetIntEdge(\"%s\")",name);
	}
}
