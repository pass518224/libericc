package hisdroid.value;

import hisdroid.TriLogic;

public class BooleanValue extends GeneralValue{
	boolean value;
	boolean bottom;
	
	public BooleanValue(){
		bottom = true;
	}
	
	public BooleanValue(boolean value){
		this.value = value;
		bottom = false;
	}
	
	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (otherValue instanceof TopValue) return this;
		if (otherValue instanceof BooleanValue) {
			BooleanValue obv = (BooleanValue) otherValue;
			if (bottom) return this;
			if (obv.bottom) return obv;
			if (value != obv.value) return new BooleanValue();
			return this;
		}
		return BottomValue.v();
	}

	@Override
	public TriLogic triLogic(){
		if (bottom) return TriLogic.Unknown;
		return TriLogic.toTriLogic(value);
	}
	
	public boolean value() { return value; }
	public boolean bottom() { return bottom; }
	
	@Override
	public boolean equals(Object o){
		if (o instanceof BooleanValue) {
			BooleanValue obv = (BooleanValue) o;
			return bottom && obv.bottom || !bottom && !obv.bottom && value == obv.value;
		}
		return false;
	}
	
	@Override
	public String toString(){
		if (bottom) return "Unknown Boolean";
		return String.valueOf(value);
	}
}
