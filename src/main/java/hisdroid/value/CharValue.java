package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;

public class CharValue extends GeneralValue{
	Set<Character> valueSet;
	boolean bottom;
	
	public CharValue(){
		this(new HashSet<Character>());
	}
	
	public CharValue(Character v){
		this(Collections.singleton(v));
	}
	
	public CharValue(Set<Character> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof CharValue) {
			CharValue oiv = (CharValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Character> newSet = new HashSet<Character>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new CharValue(newSet);
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
	
	public Set<Character> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof CharValue) {
			CharValue oiv = (CharValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Character";
		return valueSet.toString();
	}
}
