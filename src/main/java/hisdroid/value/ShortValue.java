package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;

public class ShortValue extends GeneralValue{
	Set<Short> valueSet;
	boolean bottom;
	
	public ShortValue(){
		this(new HashSet<Short>());
	}
	
	public ShortValue(Short v){
		this(Collections.singleton(v));
	}
	
	public ShortValue(Set<Short> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof ShortValue) {
			ShortValue oiv = (ShortValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Short> newSet = new HashSet<Short>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new ShortValue(newSet);
		}
		return BottomValue.v();
	}
	

	@Override
	public TriLogic triLogic(){
		if (bottom) return TriLogic.Unknown;
		else if (valueSet.size() == 1 && valueSet.contains(0)) return TriLogic.False;
		else if (valueSet.size() >= 1 && !valueSet.contains(0)) return TriLogic.True;
		return TriLogic.Unknown;
	}
	
	public Set<Short> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof ShortValue) {
			ShortValue oiv = (ShortValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}

	@Override
	public String toString(){
		if (bottom) return "Unknown Short";
		return valueSet.toString();
	}
}
