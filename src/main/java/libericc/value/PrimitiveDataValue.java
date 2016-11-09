package libericc.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import libericc.TriLogic;
import libericc.value.interfaces.ComparableGeneralValue;

public class PrimitiveDataValue<T extends Comparable<T>> extends DataValue<T> implements ComparableGeneralValue {
	
	public PrimitiveDataValue(Class<T> type){
		super(type);
	}
	
	public PrimitiveDataValue(Class<T> type, T v){
		super(type, v);
	}
	
	public PrimitiveDataValue(Class<T> type, Set<T> vset){
		super(type, vset);
	}

	@Override
	public TriLogic triLogic(){
		if (type.equals(Integer.class) || type.equals(Short.class) || type.equals(Byte.class)) {
			if (bottom) return TriLogic.Unknown;
			else if (valueSet.size() == 1 && valueSet.contains(0)) return TriLogic.False;
			else if (valueSet.size() >= 1 && !valueSet.contains(0)) return TriLogic.True;
		}
		else if (type.equals(Long.class)) {
			if (bottom) return TriLogic.Unknown;
			else if (valueSet.size() == 1 && valueSet.contains(0L)) return TriLogic.False;
			else if (valueSet.size() >= 1 && !valueSet.contains(0L)) return TriLogic.True;
		}
		else if (type.equals(Boolean.class)) {
			if (bottom) return TriLogic.Unknown;
			else if (valueSet.size() == 1 && valueSet.contains(false)) return TriLogic.False;
			else if (valueSet.size() == 1 && valueSet.contains(true)) return TriLogic.True;
		}
		return TriLogic.Unknown;
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
