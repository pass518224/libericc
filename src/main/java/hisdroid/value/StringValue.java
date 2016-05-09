package hisdroid.value;

import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;
import hisdroid.value.interfaces.EqualableGeneralValue;

public class StringValue extends DataValue<String> implements EqualableGeneralValue {
	Set<String> valueSet;
	boolean bottom;
	
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
	public TriLogic eqTo(EqualableGeneralValue rhs) {
		if (rhs instanceof StringValue) {
			StringValue srhs = (StringValue) rhs;
			if (valueSet.size() == 1 && srhs.valueSet().size() == 1){
				return TriLogic.toTriLogic(valueSet.iterator().next().equals(srhs.valueSet().iterator().next()));
			}
			Set<String> intersection = new HashSet<String>(valueSet);
			intersection.retainAll(srhs.valueSet());
			if (intersection.isEmpty()) return TriLogic.False;
		}
		return TriLogic.Unknown;
	}
}
