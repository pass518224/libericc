package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hisdroid.TriLogic;
import hisdroid.value.interfaces.ComparableGeneralValue;

public class PrimitiveDataValue<T extends Comparable<T>> extends GeneralValue implements ComparableGeneralValue {
	Class<T> type;
	Set<T> valueSet;
	boolean bottom;
	
	public PrimitiveDataValue(Class<T> type){
		this(type, new HashSet<T>());
	}
	
	public PrimitiveDataValue(Class<T> type, T v){
		this(type, Collections.singleton(v));
	}
	
	public PrimitiveDataValue(Class<T> type, Set<T> vset){
		this.type = type;
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof PrimitiveDataValue) {
			if (type.equals(((PrimitiveDataValue<?>) otherValue).type)) {
				@SuppressWarnings("unchecked")
				PrimitiveDataValue<T> other = (PrimitiveDataValue<T>) otherValue;
				if (bottom) return this;
				if (other.bottom) return other;
				Set<T> newSet = new HashSet<T>(valueSet);
				newSet.addAll(other.valueSet);
				return new PrimitiveDataValue<T>(type, newSet);
			}
		}
		return BottomValue.v();
	}
	

	@Override
	public TriLogic triLogic(){
		if (type.equals(Integer.class) || type.equals(Short.class) || type.equals(Byte.class)) {
			if (bottom) return TriLogic.Unknown;
			else if (valueSet.size() == 1 && valueSet.contains(0)) return TriLogic.False;
			else if (valueSet.size() >= 1 && !valueSet.contains(0)) return TriLogic.True;
		}
		if (type.equals(Long.class)) {
			if (bottom) return TriLogic.Unknown;
			else if (valueSet.size() == 1 && valueSet.contains(0L)) return TriLogic.False;
			else if (valueSet.size() >= 1 && !valueSet.contains(0L)) return TriLogic.True;
		}
		return TriLogic.Unknown;
	}
	
	public Class<T> type() { return type; }
	public Set<T> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof PrimitiveDataValue) {
			if (type.equals(((PrimitiveDataValue<?>)o).type)) {
				@SuppressWarnings("unchecked")
				PrimitiveDataValue<T> other = (PrimitiveDataValue<T>) o;
				return bottom && other.bottom || !bottom && !other.bottom && valueSet.equals(other.valueSet);
			}
		}
		return false;
	}

	@Override
	public String toString(){
		if (bottom) return "Unknown " + type;
		return type + ": " + valueSet.toString();
	}
	
	
	@Override
	public Set<Integer> cmpTo(ComparableGeneralValue rhs) {
		Set<Integer> res = new HashSet<Integer>();
		if (rhs instanceof PrimitiveDataValue) {
			PrimitiveDataValue<?> pdrhs = (PrimitiveDataValue<?>) rhs;
			if (!bottom && !pdrhs.bottom && type.equals(pdrhs.type)) {
				@SuppressWarnings("unchecked")
				PrimitiveDataValue<T> trhs = (PrimitiveDataValue<T>) pdrhs;
				Set<T> rhsSet = trhs.valueSet();
				
				Set<T> intersection = new HashSet<T>(valueSet);
				intersection.retainAll(rhsSet);
				if (rhsSet.size()>0) res.add(0);
				if (Collections.max(valueSet).compareTo(Collections.min(rhsSet)) > 0) res.add(1);
				if (Collections.min(valueSet).compareTo(Collections.max(rhsSet)) < 0) res.add(-1);
				
				return res;
			}
		}
		res.add(-1);
		res.add(0);
		res.add(1);
		return res;
	}
}
