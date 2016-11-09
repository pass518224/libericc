package libericc.edgefunc;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import libericc.ValueType;
import libericc.value.*;
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
	
	@SuppressWarnings("unchecked")
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof PrimitiveDataValue) {
			PrimitiveDataValue<?> pdvs = (PrimitiveDataValue<?>) source;  
			switch (t) {
			case Int:
				if (pdvs.type().equals(Integer.class)) {
					return pdvs;
				}
				else if (pdvs.type().equals(Float.class)) {
					PrimitiveDataValue<Float> v = (PrimitiveDataValue<Float>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Integer>(Integer.class);
					Set<Integer> s = new HashSet<Integer>();
					for (float x: v.valueSet()) s.add((int)x);
					return new PrimitiveDataValue<Integer>(Integer.class, s);
				}
				else if (pdvs.type().equals(Double.class)) {
					PrimitiveDataValue<Double> v = (PrimitiveDataValue<Double>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Integer>(Integer.class);
					Set<Integer> s = new HashSet<Integer>();
					for (double x: v.valueSet()) s.add((int)x);
					return new PrimitiveDataValue<Integer>(Integer.class, s);
				}
				else if (pdvs.type().equals(Long.class)) {
					PrimitiveDataValue<Long> v = (PrimitiveDataValue<Long>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Integer>(Integer.class);
					Set<Integer> s = new HashSet<Integer>();
					for (long x: v.valueSet()) s.add((int)x);
					return new PrimitiveDataValue<Integer>(Integer.class, s);
				}
				break;
			case Long:
				if (pdvs.type().equals(Long.class)) {
					return pdvs;
				}
				else if (pdvs.type().equals(Integer.class)) {
					PrimitiveDataValue<Integer> v = (PrimitiveDataValue<Integer>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Long>(Long.class);
					Set<Long> s = new HashSet<Long>();
					for (long x: v.valueSet()) s.add((long)x);
					return new PrimitiveDataValue<Long>(Long.class, s);
				}
				else if (pdvs.type().equals(Float.class)) {
					PrimitiveDataValue<Float> v = (PrimitiveDataValue<Float>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Long>(Long.class);
					Set<Long> s = new HashSet<Long>();
					for (float x: v.valueSet()) s.add((long)x);
					return new PrimitiveDataValue<Long>(Long.class, s);
				}
				else if (pdvs.type().equals(Double.class)) {
					PrimitiveDataValue<Double> v = (PrimitiveDataValue<Double>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Long>(Long.class);
					Set<Long> s = new HashSet<Long>();
					for (double x: v.valueSet()) s.add((long)x);
					return new PrimitiveDataValue<Long>(Long.class, s);
				}
				break;
			case Double:
				if (pdvs.type().equals(Double.class)) {
					return pdvs;
				}
				else if (pdvs.type().equals(Integer.class)) {
					PrimitiveDataValue<Integer> v = (PrimitiveDataValue<Integer>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Double>(Double.class);
					Set<Double> s = new HashSet<Double>();
					for (int x: v.valueSet()) s.add((double)x);
					return new PrimitiveDataValue<Double>(Double.class, s);
				}
				else if (pdvs.type().equals(Long.class)) {
					PrimitiveDataValue<Long> v = (PrimitiveDataValue<Long>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Double>(Double.class);
					Set<Double> s = new HashSet<Double>();
					for (long x: v.valueSet()) s.add((double)x);
					return new PrimitiveDataValue<Double>(Double.class, s);
				}
				else if (pdvs.type().equals(Float.class)) {
					PrimitiveDataValue<Float> v = (PrimitiveDataValue<Float>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Double>(Double.class);
					Set<Double> s = new HashSet<Double>();
					for (float x: v.valueSet()) s.add((double)x);
					return new PrimitiveDataValue<Double>(Double.class, s);
				}
				break;
			case Float:
				if (pdvs.type().equals(Float.class)) {
					return pdvs;
				}
				else if (pdvs.type().equals(Integer.class)) {
					PrimitiveDataValue<Integer> v = (PrimitiveDataValue<Integer>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Float>(Float.class);
					Set<Float> s = new HashSet<Float>();
					for (int x: v.valueSet()) s.add((float)x);
					return new PrimitiveDataValue<Float>(Float.class, s);
				}
				else if (pdvs.type().equals(Long.class)) {
					PrimitiveDataValue<Long> v = (PrimitiveDataValue<Long>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Float>(Float.class);
					Set<Float> s = new HashSet<Float>();
					for (long x: v.valueSet()) s.add((float)x);
					return new PrimitiveDataValue<Float>(Float.class, s);
				}
				else if (pdvs.type().equals(Double.class)) {
					PrimitiveDataValue<Double> v = (PrimitiveDataValue<Double>) pdvs;
					if (v.bottom()) return new PrimitiveDataValue<Float>(Float.class);
					Set<Float> s = new HashSet<Float>();
					for (double x: v.valueSet()) s.add((float)x);
					return new PrimitiveDataValue<Float>(Float.class, s);
				}
				break;
			case Short:
			case Byte:
			case Boolean:
			case Char:
			default:
			}
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
