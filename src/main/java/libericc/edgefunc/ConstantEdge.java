package libericc.edgefunc;

import heros.EdgeFunction;
import libericc.value.*;
/**
 * An edge return a constant value
 * @author chchao
 *
 */
public class ConstantEdge extends EdgeFunctionTemplate {
	protected GeneralValue c;

	public ConstantEdge(GeneralValue constant) {
		c = constant;
	}
	
	public ConstantEdge(GeneralValue constant, EdgeFunction<GeneralValue> next) {
		c = constant;
		if (next != null) composeWith(next);
	}
	
	@Override
	public EdgeFunctionTemplate copy(){
		return new ConstantEdge(c, next);
	}
	
	@Override
	public GeneralValue computeTargetImplementation(GeneralValue source) {
		return c;
	}

	@Override
	public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction){
		if (otherFunction instanceof ConstantEdge) {
			ConstantEdge otherConstantEdge = (ConstantEdge) otherFunction;
			return new ConstantEdge(c.joinWith(otherConstantEdge.c), next);
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}

	@Override
	public boolean equalToFirstEdge(EdgeFunction<GeneralValue> other) {
		if (other instanceof ConstantEdge) {
			ConstantEdge otherConstant = (ConstantEdge) other;
			return c.equals(otherConstant.c);
		}
		return false;
	}
	
	@Override
	public String edgeToString(){
		return "ConstantEdge("+c+")";
	}
	
}
