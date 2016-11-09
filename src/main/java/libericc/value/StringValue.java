package libericc.value;

import java.util.HashSet;
import java.util.Set;

import libericc.PredictResult;
import libericc.value.interfaces.EqualableGeneralValue;

public class StringValue extends DataValue<String> implements EqualableGeneralValue {
	
	public StringValue(){
		super(String.class);
	}
		
	public StringValue(String v){
		super(String.class, v);
	}
	
	public StringValue(Set<String> vset){
		super(String.class, vset);
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown String";
		return valueSet.toString();
	}

	@Override
	public PredictResult eqTo(EqualableGeneralValue rhs) {
		if (rhs instanceof StringValue) {
			StringValue srhs = (StringValue) rhs;
			if (bottom || srhs.bottom()) return PredictResult.Ambiguous;
			if (valueSet.isEmpty() || valueSet.isEmpty()) return PredictResult.Ambiguous;
			if (valueSet.size() == 1 && srhs.valueSet().size() == 1){
				return PredictResult.toPredictResult(valueSet.iterator().next().equals(srhs.valueSet().iterator().next()));
			}
			Set<String> intersection = new HashSet<String>(valueSet);
			intersection.retainAll(srhs.valueSet());
			if (intersection.isEmpty()) return PredictResult.False;
			return PredictResult.Both;
		}
		return PredictResult.Unknown;
	}
}
