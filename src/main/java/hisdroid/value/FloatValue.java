package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FloatValue extends GeneralValue{
	Set<Float> valueSet;
	boolean bottom;
	
	public FloatValue(){
		this(new HashSet<Float>());
	}
	
	public FloatValue(Float v){
		this(Collections.singleton(v));
	}
	
	public FloatValue(Set<Float> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof FloatValue) {
			FloatValue oiv = (FloatValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Float> newSet = new HashSet<Float>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new FloatValue(newSet);
		}
		return BottomValue.v();
	}
	
	public Set<Float> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof FloatValue) {
			FloatValue oiv = (FloatValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}

	@Override
	public String toString(){
		if (bottom) return "Unknown Float";
		return valueSet.toString();
	}
}
