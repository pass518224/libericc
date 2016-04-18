package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;

public class ByteValue extends GeneralValue{
	Set<Byte> valueSet;
	boolean bottom;
	
	public ByteValue(){
		this(new HashSet<Byte>());
	}
	
	public ByteValue(Byte v){
		this(Collections.singleton(v));
	}
	
	public ByteValue(Set<Byte> vset){
		valueSet = vset;
		bottom = vset.isEmpty();
	}

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof ByteValue) {
			ByteValue oiv = (ByteValue) otherValue;
			if (bottom) return this;
			if (oiv.bottom) return oiv;
			Set<Byte> newSet = new HashSet<Byte>(valueSet);
			newSet.addAll(oiv.valueSet);
			return new ByteValue(newSet);
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
	
	public Set<Byte> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof ByteValue) {
			ByteValue oiv = (ByteValue) o;
			return bottom && oiv.bottom || !bottom && !oiv.bottom && valueSet.equals(oiv.valueSet);
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Byte";
		return valueSet.toString();
	}
}
