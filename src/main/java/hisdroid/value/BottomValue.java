package hisdroid.value;


public class BottomValue extends GeneralValue {
	private final static BottomValue instance = new BottomValue();
	
	private BottomValue(){}
	
	public static BottomValue v() {
		return instance;
	}

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		return this;
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
		return false;
	}
	
	@Override
	public String toString(){
		return "bottom";
	}
	
}
