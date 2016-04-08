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
import hisdroid.value.StringValueSet;

public class IntentGetStringEdge extends EdgeFunctionTemplate {
	String name;
	
	public IntentGetStringEdge(){
		this.name = "";
	}
	
	public IntentGetStringEdge(String name){
		this.name = name;
	}

	public IntentGetStringEdge(String name, EdgeFunction<GeneralValue> next){
		this.name = name;
		this.next = next;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public EdgeFunctionTemplate copy() {
		return new IntentGetStringEdge(name, next);
	}

	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof IntentValue) {
			IntentValue intentSource = (IntentValue) source;
			Set<String> stringSet = new HashSet<String>();
			for (JSONObject i: intentSource.intents()) {
				stringSet.add(i.getJSONObject("Extras").getString(name));
			}
			return new StringValueSet(stringSet);
		}
		else if (source instanceof BundleValue) {
			BundleValue bundleSource = (BundleValue) source;
			Set<String> stringSet = new HashSet<String>();
			for (JSONObject i: bundleSource.bundles()) {
				stringSet.add(i.getString(name));
			}
			return new StringValueSet(stringSet);
		}
		return new IntValueSet();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction) {
		if (otherFunction instanceof IntentGetStringEdge) {
			IntentGetStringEdge otherStringEdge = (IntentGetStringEdge) otherFunction;
			if (name.equals(otherStringEdge.name)) return this;
			return new IntentGetStringEdge();
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof IntentGetStringEdge) {
			IntentGetStringEdge otherStringEdge = (IntentGetStringEdge) other;
			return name.equals(otherStringEdge.name);
		}
		return false;
	}

	@Override
	public String edgeToString() {
		return "GetStringEdge";
	}

}
