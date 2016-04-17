package hisdroid.value;

public class NullValue extends GeneralValue {
	private final static NullValue instance = new NullValue();
	
	private NullValue(){}
	
	public static NullValue v() {
		return instance;
	}

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		if (equals(otherValue)) return this;
		return BottomValue.v();
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
		return false;
	}
	
	@Override
	public String toString(){
		return "null";
	}
}
