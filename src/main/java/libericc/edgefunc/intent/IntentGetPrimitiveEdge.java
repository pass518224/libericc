package libericc.edgefunc.intent;

import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import libericc.Utility;
import libericc.value.GeneralValue;
import libericc.value.PrimitiveDataValue;

public abstract class IntentGetPrimitiveEdge<T extends Comparable<T>> extends IntentGetDataEdge<T> {
	
	public IntentGetPrimitiveEdge(Class<T> type){
		super(type);
	}
	
	public IntentGetPrimitiveEdge(Class<T> type, String name){
		super(type, name);
	}
	
	public IntentGetPrimitiveEdge(Class<T> type, String name, T defaultValue){
		super(type, name, defaultValue);
	}

	protected IntentGetPrimitiveEdge(IntentGetPrimitiveEdge<T> old, EdgeFunction<GeneralValue> next){
		super(old, next);
	}
	
	abstract T stringToT(String s);

	@Override
	GeneralValue unknownGeneralValue() {
		return new PrimitiveDataValue<T>(type);
	}
	
	@Override
	GeneralValue knownGeneralValue(Set<T> vset) {
		return new PrimitiveDataValue<T>(type, vset);
	}

	@Override
	String parcelTypeNumber() {
		return String.valueOf(Utility.getParcelTypeNumber(type));
	}

	@Override
	T mappedValueToT(JSONObject v) {
		return stringToT(v.getString("_"));
	}
}
