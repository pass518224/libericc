package hisdroid.edgefunc;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.Utility;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntentValue;
import hisdroid.value.PrimitiveDataValue;

public class GetIntentEdge extends EdgeFunctionTemplate {
	JSONObject iccLogs;
	
	public GetIntentEdge(JSONObject iccLogs) {
		this.iccLogs = iccLogs;
	}
	
	public GetIntentEdge(GetIntentEdge old, EdgeFunction<GeneralValue> next) {
		this.iccLogs = old.iccLogs;
		this.next = next; 
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new GetIntentEdge(this, next);
	}
	
	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source){
		if (source instanceof PrimitiveDataValue) {
			if (Integer.class.equals(((PrimitiveDataValue<?>)source).type())){
				@SuppressWarnings("unchecked")
				PrimitiveDataValue<Integer> intSetSource = (PrimitiveDataValue<Integer>) source;
				if (!intSetSource.bottom()) {
					Set<JSONObject> intentSet = new HashSet<JSONObject>();
					for (Integer i: intSetSource.valueSet()) {
						JSONObject jo = Utility.iccToIntent(iccLogs.getJSONObject(String.valueOf(i)));
						if (jo != null) intentSet.add(jo);
					}
					return new IntentValue(intentSet);
				}
			}
		}
		return new IntentValue();
	}
	
	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof GetIntentEdge) {
			GetIntentEdge otherIntentEdge = (GetIntentEdge) otherFunction;
			if (equals(otherIntentEdge)) return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}
	
	@Override
	protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other){
		if (other instanceof GetIntentEdge) {
			GetIntentEdge otherIntentEdge = (GetIntentEdge) other;
			return otherIntentEdge.iccLogs == iccLogs;
		}
		return false;
	}
	
	@Override
	public String edgeToString(){
		return "GetIntentEdge()";
	}
}
