package hisdroid.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DataValue<T> extends GeneralValue {
	Class<T> type;
	Set<T> valueSet;
	boolean bottom;
	
	public DataValue(Class<T> type){
		this(type, new HashSet<T>());
	}
	
	public DataValue(Class<T> type, T v){
		this(type, Collections.singleton(v));
	}
	
	public DataValue(Class<T> type, Set<T> vset){
		this.type = type;
		valueSet = vset;
		bottom = vset.isEmpty();
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof DataValue) {
			if (type.equals(((DataValue<?>) otherValue).type)) {
				@SuppressWarnings("unchecked")
				DataValue<T> other = (DataValue<T>) otherValue;
				if (bottom) return this;
				if (other.bottom) return other;
				Set<T> newSet = new HashSet<T>(valueSet);
				newSet.addAll(other.valueSet);
				return new DataValue<T>(type, newSet);
			}
		}
		return BottomValue.v();
	}
	
	public Class<T> type() { return type; }
	public Set<T> valueSet() { return valueSet; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof DataValue) {
			if (type.equals(((DataValue<?>)o).type)) {
				@SuppressWarnings("unchecked")
				DataValue<T> other = (DataValue<T>) o;
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
}
