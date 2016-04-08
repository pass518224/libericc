package hisdroid.value;

public class TopValue extends GeneralValue{
	private final static TopValue instance = new TopValue();
	
	private TopValue(){}
	
	public static TopValue v() {
		return instance;
	}

	@Override
	public GeneralValue joinWith(GeneralValue otherValue){
		return otherValue;
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof TopValue) return true;
		return false;
	}
	
	@Override
	public String toString(){
		return "top";
	}
	
}
