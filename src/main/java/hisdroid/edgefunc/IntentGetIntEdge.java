package hisdroid.edgefunc;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.value.BottomValue;
import hisdroid.value.BundleValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValueSet;
import hisdroid.value.IntentValue;

public class IntentGetIntEdge extends EdgeFunctionTemplate {
	String name;
	
	public IntentGetIntEdge(){
		this.name = "";
	}
	
	public IntentGetIntEdge(String name){
		this.name = name;
	}

	public IntentGetIntEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetIntEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue) {
			IntentValue intentSource = (IntentValue) source;
			Set<Integer> intSet = new HashSet<Integer>();
			for (JSONObject i: intentSource.intents()) {
				try {
					intSet.add(Integer.valueOf(i.getJSONObject("Extras").getJSONObject(name).getString("Value")));
				}
				catch (Exception e) {}
			}
			return new IntValueSet(intSet);
		}
		else if (source instanceof BundleValue) {
			BundleValue bundleSource = (BundleValue) source;
			Set<Integer> intSet = new HashSet<Integer>();
			for (JSONObject i: bundleSource.bundles()) {
				intSet.add(i.getInt(name));
			}
			return new IntValueSet(intSet);
		}
		return new IntValueSet();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetIntEdge) {
			IntentGetIntEdge otherIntEdge = (IntentGetIntEdge) otherFunction;
			if (name.equals(otherIntEdge.name)) return this;
			return new IntentGetIntEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetIntEdge) {
			IntentGetIntEdge otherIntEdge = (IntentGetIntEdge) other;
			return name.equals(otherIntEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		return String.format("GetIntEdge(\"%s\")",name);
	}

}
