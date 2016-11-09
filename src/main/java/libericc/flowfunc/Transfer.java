package libericc.flowfunc;

import java.util.Set;

import heros.FlowFunction;

import static heros.TwoElementSet.twoElementSet;
import java.util.Collections;

public class Transfer<D> implements FlowFunction<D> {
	
	private final D toValue;
	private final D fromValue;
	
	public Transfer(D toValue, D fromValue){
		this.toValue = toValue;
		this.fromValue = fromValue;
	} 

	public Set<D> computeTargets(D source) {
		if(source.equals(fromValue)) {
			return twoElementSet(source, toValue);
		} else if(source.equals(toValue)) {
			return Collections.emptySet();
		} else {
			return Collections.singleton(source);
		}
	}
}