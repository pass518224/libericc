package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StringValueSet extends GeneralValue{
	Set<String> valueSet;
	boolean bottom;
	
	public StringValueSet(){
		bottom = true;
	}
		
	public StringValueSet(String v){
		this(Collections.singleton(v));
	}
	
	public StringValueSet(Set<String> vset){
		valueSet = vset;
		bottom = false;
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof StringValueSet) {
			StringValueSet osv = (StringValueSet) otherValue;
			Set<String> newSet = new HashSet<String>(valueSet);
			newSet.addAll(osv.valueSet);
			return new StringValueSet(newSet);
		}
		return BottomValue.v();
	}
	
	public Set<String> value() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof StringValueSet) {
			StringValueSet osv = (StringValueSet) o;
			return bottom && osv.bottom || !bottom && !osv.bottom && valueSet.equals(osv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown String";
		return valueSet.toString();
	}
}
