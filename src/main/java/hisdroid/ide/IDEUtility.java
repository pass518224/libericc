package hisdroid.ide;

import heros.EdgeFunction;
import hisdroid.edgefunc.ConstantEdge;
import hisdroid.value.*;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;

public class IDEUtility {
	IDEProblem p;
	
	public IDEUtility(IDEProblem problem) {
		p = problem;
	}
	
	public EdgeFunction<GeneralValue> getEdgeFromConstant(Constant c){
		if (c instanceof IntConstant) {
			return new ConstantEdge(new PrimitiveDataValue<Integer>(Integer.class, ((IntConstant)c).value));
		}
		if (c instanceof LongConstant) {
			return new ConstantEdge(new PrimitiveDataValue<Long>(Long.class, ((LongConstant)c).value));
		}
		if (c instanceof FloatConstant) {
			return new ConstantEdge(new PrimitiveDataValue<Float>(Float.class, ((FloatConstant)c).value));
		}
		if (c instanceof DoubleConstant) {
			return new ConstantEdge(new PrimitiveDataValue<Double>(Double.class, ((DoubleConstant)c).value));
		}
		if (c instanceof StringConstant) {
			return new ConstantEdge(new StringValue(((StringConstant)c).value));
		}
		if (c instanceof NullConstant) {
			return new ConstantEdge(new NullValue());
		}
		return p.allTopFunction();
	}
}
