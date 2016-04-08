package hisdroid.edgefunc;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import heros.EdgeFunction;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValueSet;
import hisdroid.value.IntentValue;

public class GetIntentEdge extends EdgeFunctionTemplate {
	JSONObject iccLogs;
	
	public GetIntentEdge(JSONObject iccLogs) {
		this.iccLogs = iccLogs;
	}
	
	public GetIntentEdge(JSONObject iccLogs, EdgeFunction<GeneralValue> next) {
		this.iccLogs = iccLogs;
		this.next = next; 
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new GetIntentEdge(iccLogs, next);
	}
	
	@Override
	protected GeneralValue computeTargetImplementation(GeneralValue source){
		if (source instanceof IntValueSet) {
			IntValueSet intSetSource = (IntValueSet) source;
			if (!intSetSource.bottom()) {
				Set<JSONObject> intentSet = new HashSet<JSONObject>();
				for (Integer i: intSetSource.valueSet()) {
					intentSet.add(iccLogs.getJSONObject(String.valueOf(i)));
				}
				return new IntentValue(intentSet);
			}
		}
		return new IntentValue();
	}
	
	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof GetIntentEdge) {
			GetIntentEdge otherIntentEdge = (GetIntentEdge) otherFunction;
			if (otherIntentEdge.iccLogs == iccLogs) return this;
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
		return "GetIntentEdge";
	}
}
