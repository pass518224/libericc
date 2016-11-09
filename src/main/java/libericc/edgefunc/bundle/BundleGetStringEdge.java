package libericc.edgefunc.bundle;

import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.edgefunc.EdgeFunctionTemplate;
import libericc.value.GeneralValue;
import libericc.value.StringValue;

public class BundleGetStringEdge extends BundleGetDataEdge<String> {
	
	public BundleGetStringEdge(){
		super(String.class);
	}
	
	public BundleGetStringEdge(String name){
		super(String.class, name);
	}

	public BundleGetStringEdge(String name, String defaultValue){
		super(String.class, name, defaultValue);
	}
	
	public BundleGetStringEdge(BundleGetStringEdge old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new BundleGetStringEdge(this, next);
	}

	@Override
	GeneralValue unknownGeneralValue() {
		return new StringValue();
	}
	
	@Override
	GeneralValue knownGeneralValue(Set<String> vset) {
		return new StringValue(vset);
	}

	@Override
	String parcelTypeNumber() {
		return "0";
	}

	@Override
	String mappedValueToT(JSONObject v) {
		return v.getString("_");
	}

	@Override
	public String edgeToString() {
		if (!knownName) return "BundleGetStringEdge()";
		else return String.format("BundleGetStringEdge(\"%s\")", name);
	}

}
