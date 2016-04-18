package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StringValue extends GeneralValue{
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
	
	public Set<String> value() { return valueSet; }
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
}
