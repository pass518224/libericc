package hisdroid.edgefunc;
import heros.EdgeFunction; 

/**
 * Change the implementation of joinWith
 * @author chchao
 *
 * @param <V>
 */
public class AllBottom<V> extends heros.edgefunc.AllBottom<V>{
	
	public AllBottom(V bottomElement){
		super(bottomElement);
	}
	
	public EdgeFunction<V> joinWith(EdgeFunction<V> otherFunction) {
		return this;
	}
	
}
