package libericc.edgefunc;

import java.util.HashSet;
import java.util.Set;

import heros.EdgeFunction;
import libericc.value.BottomValue;
import libericc.value.GeneralValue;
import libericc.value.PrimitiveDataValue;
import libericc.value.StringValue;

public class StringHashCodeEdge extends EdgeFunctionTemplate {
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new StringHashCodeEdge();
	}
	
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		if (source instanceof StringValue) {
			StringValue strvalue = (StringValue) source;
			if (!strvalue.bottom()) {
				Set<Integer> hashcodes = new HashSet<Integer>();
				for (String str: strvalue.valueSet()) {
					hashcodes.add(str.hashCode());
				}
				return new PrimitiveDataValue<Integer>(Integer.class, hashcodes);
			}
		}
		return new PrimitiveDataValue<Integer>(Integer.class);
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof StringHashCodeEdge) {
			return this;
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	public boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		return other instanceof StringHashCodeEdge;
	}
	
	@Override
	public String edgeToString(){
		return "StringHashCodeEdge";
	}
}
