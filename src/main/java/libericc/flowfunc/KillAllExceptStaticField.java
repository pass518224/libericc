package libericc.flowfunc;

import java.util.Collections;
import java.util.Set;

import heros.FlowFunction;
import soot.Value;
import soot.jimple.StaticFieldRef;

public class KillAllExceptStaticField implements FlowFunction<Value> {

	private final static KillAllExceptStaticField instance = new KillAllExceptStaticField();
	
	private KillAllExceptStaticField(){} //use v() instead

	public static KillAllExceptStaticField v() {
		return instance;
	}
	
	public Set<Value> computeTargets(Value source) {
		if (source instanceof StaticFieldRef) {
			return Collections.singleton(source);
		}
		return Collections.emptySet();
	}
}
