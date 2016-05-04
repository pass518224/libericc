package hisdroid.edgefunc;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import heros.EdgeFunction;
import hisdroid.value.*;
import soot.jimple.FloatConstant;
/**
 * An edge return a constant value
 * @author chchao
 *
 */
public class CompareFloatEdge extends EdgeFunctionTemplate {
	FloatConstant c;
	boolean reversed;
	// true: value cmp constant
	// false: constant cmp value
	
	public CompareFloatEdge(FloatConstant constant, boolean reversed) {
		c = constant;
		this.reversed = reversed;
	}
	
	public CompareFloatEdge(CompareFloatEdge old, EdgeFunction<GeneralValue> next) {
		c = old.c;
		reversed = old.reversed;
		if (next != null) composeWith(next);
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new CompareFloatEdge(this, next);
	}
	
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof FloatValue) {
			FloatValue v = (FloatValue) source;
			if (v.bottom()) return new IntValue();
			Set<Integer> res = new TreeSet<Integer>();
			Set<Float> vset = v.valueSet();
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
		if (otherFunction instanceof CompareFloatEdge) {
			CompareFloatEdge otherEdge = (CompareFloatEdge) otherFunction;
			if (equals(otherEdge)) return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	public boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof CompareFloatEdge) {
			CompareFloatEdge otherEdge = (CompareFloatEdge) other;
			return c.equals(otherEdge.c) && reversed == otherEdge.reversed;
		}
		return false;
	}
	
	@Override
	public String edgeToString(){
		if (!reversed) {
			return "CompareFloatEdge(FloatValue, "+c+")";
		}
		else {
			return "CompareFloatEdge("+c+", FloatValue)";
		}
	}
}
