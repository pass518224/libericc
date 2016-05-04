package hisdroid.edgefunc;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import heros.EdgeFunction;
import hisdroid.value.*;
import soot.jimple.LongConstant;
/**
 * An edge return a constant value
 * @author chchao
 *
 */
public class CompareLongEdge extends EdgeFunctionTemplate {
	LongConstant c;
	boolean reversed;
	// false: value cmp constant
	// true: constant cmp value
	
	public CompareLongEdge(LongConstant constant, boolean reversed) {
		c = constant;
		this.reversed = reversed;
	}
	
	public CompareLongEdge(CompareLongEdge old, EdgeFunction<GeneralValue> next) {
		c = old.c;
		reversed = old.reversed;
		if (next != null) composeWith(next);
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new CompareLongEdge(this, next);
	}
	
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof LongValue) {
			LongValue v = (LongValue) source;
			if (v.bottom()) return new IntValue();
			Set<Integer> res = new TreeSet<Integer>();
			Set<Long> vset = v.valueSet();
			if (vset.contains(c.value)) res.add(0);
			if (!reversed) {
				if (Collections.max(vset) > c.value) res.add(1);
				if (Collections.min(vset) < c.value) res.add(-1);
			}
			else {
				if (Collections.max(vset) > c.value) res.add(-1);
				if (Collections.min(vset) < c.value) res.add(1);
			}
			return new IntValue(res);
		}
		return new IntValue();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof CompareLongEdge) {
			CompareLongEdge otherEdge = (CompareLongEdge) otherFunction;
			if (equals(otherEdge)) return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	public boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof CompareLongEdge) {
			CompareLongEdge otherEdge = (CompareLongEdge) other;
			return c.equals(otherEdge.c) && reversed == otherEdge.reversed;
		}
		return false;
	}
	
	@Override
	public String edgeToString(){
		if (!reversed) {
			return "CompareLongEdge(LongValue, "+c+")";
		}
		else {
			return "CompareLongEdge("+c+", LongValue)";
		}
	}
}
