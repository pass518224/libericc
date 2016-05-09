package hisdroid.value;

import soot.Unit;
import soot.Value;

public class EqualityValue extends GeneralValue {
	Unit u;
	Value lhs, rhs;
	
	public EqualityValue(Unit u, Value lhs, Value rhs){
		this.u = u;
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	public Unit unit() { return u; }
	public Value lhs() { return lhs; }
	public Value rhs() { return rhs; }
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue) {
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof EqualityValue) {
			EqualityValue cv = (EqualityValue) otherValue;
			if (equals(cv)) return this;
		}
		return BottomValue.v();
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof EqualityValue) {
			EqualityValue cv = (EqualityValue) o;
			return u.equals(cv.u) && lhs.equivTo(cv.lhs) && rhs.equivTo(cv.rhs);
		}
		return false;
	}

	@Override
	public String toString(){
		return String.format("Equality (%s, %s) at %s", lhs, rhs, u);
	}
	
}
