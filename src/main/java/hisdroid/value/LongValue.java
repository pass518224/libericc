package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;

public class LongValue extends GeneralValue{
	Set<Long> valueSet;
	boolean bottom;
	
	public LongValue(){
		this(new HashSet<Long>());
	}
	
	public LongValue(Long v){
		this(Collections.singleton(v));
	}
	
	public LongValue(Set<Long> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof LongValue) {
			LongValue oiv = (LongValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Long> newSet = new HashSet<Long>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new LongValue(newSet);
		}
		return BottomValue.v();
	}
	

	@Override
	public TriLogic triLogic(){
		if (bottom) return TriLogic.Unknown;
		else if (valueSet.size() == 1 && valueSet.contains(0L)) return TriLogic.False;
		else if (valueSet.size() >= 1 && !valueSet.contains(0L)) return TriLogic.True;
		return TriLogic.Unknown;
	}
	
	public Set<Long> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof LongValue) {
			LongValue oiv = (LongValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Long";
		return valueSet.toString();
	}
}
