package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;
import hisdroid.value.interfaces.EqualableGeneralValue;

public class StringValue extends GeneralValue implements EqualableGeneralValue {
	Set<String> valueSet;
	boolean bottom;
	
	public StringValue(){
		bottom = true;
	}
		
	public StringValue(String v){
		this(Collections.singleton(v));
	}
	
	public StringValue(Set<String> vset){
		valueSet = vset;
		bottom = false;
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof StringValue) {
			StringValue osv = (StringValue) otherValue;
			if (bottom) return this;
			if (osv.bottom) return osv;
			Set<String> newSet = new HashSet<String>(valueSet);
			newSet.addAll(osv.valueSet);
			return new StringValue(newSet);
		}
		return BottomValue.v();
	}
	
	public Set<String> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof StringValue) {
			StringValue osv = (StringValue) o;
			return bottom && osv.bottom || !bottom && !osv.bottom && valueSet.equals(osv.valueSet);
		}
		return false;
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
