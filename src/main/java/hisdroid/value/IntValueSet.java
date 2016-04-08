package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;

public class IntValueSet extends GeneralValue{
	Set<Integer> valueSet;
	boolean bottom;
	
	public IntValueSet(){
		valueSet = new HashSet<Integer>();
		bottom = true;
	}
	
	public IntValueSet(int v){
		this(Collections.singleton(v));
	}
	
	public IntValueSet(Set<Integer> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof IntValueSet) {
			IntValueSet oiv = (IntValueSet) otherValue;
			Set<Integer> newSet = new HashSet<Integer>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new IntValueSet(newSet);
		}
		return BottomValue.v();
	}
	

	@Override
	public TriLogic triLogic(){
		if (bottom) return TriLogic.Unknown;
		else if (valueSet.size() == 1 && valueSet.contains(0)) return TriLogic.False;
		else if (valueSet.size() >= 1 && !valueSet.contains(0)) return TriLogic.True;
		else return TriLogic.Unknown;
	}
	
	public Set<Integer> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof IntValueSet) {
			IntValueSet oiv = (IntValueSet) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Integer";
		return valueSet.toString();
	}
}
