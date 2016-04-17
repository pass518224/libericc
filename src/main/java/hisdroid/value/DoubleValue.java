package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DoubleValue extends GeneralValue{
	Set<Double> valueSet;
	boolean bottom;
	
	public DoubleValue(){
		this(new HashSet<Double>());
	}
	
	public DoubleValue(Double v){
		this(Collections.singleton(v));
	}
	
	public DoubleValue(Set<Double> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof DoubleValue) {
			DoubleValue oiv = (DoubleValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Double> newSet = new HashSet<Double>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new DoubleValue(newSet);
		}
		return BottomValue.v();
	}
	
	public Set<Double> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof DoubleValue) {
			DoubleValue oiv = (DoubleValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Double";
		return valueSet.toString();
	}
}
