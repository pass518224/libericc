package hisdroid.edgefunc;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import hisdroid.ValueType;
import hisdroid.value.*;
/**
 * An edge return a constant value
 * @author chchao
 *
 */
public class CastEdge extends EdgeFunctionTemplate {
	protected ValueType t;

	public CastEdge(ValueType type) {
		t = type;
	}
	
	public CastEdge(ValueType type, EdgeFunction<GeneralValue> next) {
		t = type;
		if (next != null) composeWith(next);
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new CastEdge(t, next);
	}
	
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		switch (t) {
		case Int:
			if (source instanceof IntValue) {
				return source;
			}
			else if (source instanceof FloatValue) {
				FloatValue v = (FloatValue) source;
				if (v.bottom()) return new IntValue();
				Set<Integer> s = new HashSet<Integer>();
				for (float x: v.valueSet()) s.add((int)x);
				return new IntValue(s);
			}
			else if (source instanceof DoubleValue) {
				DoubleValue v = (DoubleValue) source;
				if (v.bottom()) return new IntValue();
				Set<Integer> s = new HashSet<Integer>();
				for (double x: v.valueSet()) s.add((int)x);
				return new IntValue(s);
			}
			else if (source instanceof LongValue) {
				LongValue v = (LongValue) source;
				if (v.bottom()) return new IntValue();
				Set<Integer> s = new HashSet<Integer>();
				for (long x: v.valueSet()) s.add((int)x);
				return new IntValue(s);
			}
			break;
		case Long:
			if (source instanceof LongValue) {
				return source;
			}
			else if (source instanceof IntValue) {
				IntValue v = (IntValue) source;
				if (v.bottom()) return new LongValue();
				Set<Long> s = new HashSet<Long>();
				for (long x: v.valueSet()) s.add((long)x);
				return new LongValue(s);
			}
			else if (source instanceof FloatValue) {
				FloatValue v = (FloatValue) source;
				if (v.bottom()) return new LongValue();
				Set<Long> s = new HashSet<Long>();
				for (float x: v.valueSet()) s.add((long)x);
				return new LongValue(s);
			}
			else if (source instanceof DoubleValue) {
				DoubleValue v = (DoubleValue) source;
				if (v.bottom()) return new LongValue();
				Set<Long> s = new HashSet<Long>();
				for (double x: v.valueSet()) s.add((long)x);
				return new LongValue(s);
			}
			break;
		case Double:
			if (source instanceof DoubleValue) {
				return source;
			}
			else if (source instanceof IntValue) {
				IntValue v = (IntValue) source;
				if (v.bottom()) return new DoubleValue();
				Set<Double> s = new HashSet<Double>();
				for (int x: v.valueSet()) s.add((double)x);
				return new DoubleValue(s);
			}
			else if (source instanceof LongValue) {
				LongValue v = (LongValue) source;
				if (v.bottom()) return new DoubleValue();
				Set<Double> s = new HashSet<Double>();
				for (long x: v.valueSet()) s.add((double)x);
				return new DoubleValue(s);
			}
			else if (source instanceof FloatValue) {
				FloatValue v = (FloatValue) source;
				if (v.bottom()) return new DoubleValue();
				Set<Double> s = new HashSet<Double>();
				for (float x: v.valueSet()) s.add((double)x);
				return new DoubleValue(s);
			}
			break;
		case Float:
			if (source instanceof FloatValue) {
				return source;
			}
			else if (source instanceof IntValue) {
				IntValue v = (IntValue) source;
				if (v.bottom()) return new FloatValue();
				Set<Float> s = new HashSet<Float>();
				for (int x: v.valueSet()) s.add((float)x);
				return new FloatValue(s);
			}
			else if (source instanceof LongValue) {
				LongValue v = (LongValue) source;
				if (v.bottom()) return new FloatValue();
				Set<Float> s = new HashSet<Float>();
				for (long x: v.valueSet()) s.add((float)x);
				return new FloatValue(s);
			}
			else if (source instanceof DoubleValue) {
				DoubleValue v = (DoubleValue) source;
				if (v.bottom()) return new FloatValue();
				Set<Float> s = new HashSet<Float>();
				for (double x: v.valueSet()) s.add((float)x);
				return new FloatValue(s);
			}
			break;
		case Short:
		case Byte:
		case Boolean:
		case Char:
		default:
		}
		return BottomValue.v();
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof CastEdge && equals(otherFunction)) {
			return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	public boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof CastEdge) {
			CastEdge otherEdge = (CastEdge) other;
			return t == otherEdge.t;
		}
		return false;
	}
	
	@Override
	public String edgeToString(){
		return "CastEdge("+t+")";
	}
	
}
