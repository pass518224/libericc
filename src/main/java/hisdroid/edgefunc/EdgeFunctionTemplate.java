package hisdroid.edgefunc;

import java.util.logging.Logger;

import heros.EdgeFunction;
import heros.edgefunc.EdgeIdentity;
import hisdroid.value.BottomValue;
import hisdroid.value.GeneralValue;
/**
 * Provide default implementation of EdgeFunction with Linked List to compose edges
 * @author chchao
 *
 */
public abstract class EdgeFunctionTemplate implements EdgeFunction<GeneralValue> {
	static protected Logger logger = Logger.getLogger("HisDroid");
	/**
	 * link two edge rather than compose them
	 */
	protected EdgeFunction<GeneralValue> next = null;

	/**
	 * Copy this edge instance
	 */
	abstract public EdgeFunctionTemplate copy();
	
	/**
	 * The method become a wrapper which handle the edge list
	 * subclass should implement its own computeTarget in computeTargetImplementation()
	 */
	@Override
	final public GeneralValue computeTarget(GeneralValue source) {
		if (next == null) return computeTargetImplementation(source);
		else return next.computeTarget(computeTargetImplementation(source));
	}
	
	/**
	 * The implementation of computeTarget
	 */
	abstract protected GeneralValue computeTargetImplementation(GeneralValue source);

	/**
	 *  The method has become a wrapper and set the secondFunction at next
	 *  subclass should implement composeWithNext()
	 */
	@Override
	final public EdgeFunction<GeneralValue> composeWith(EdgeFunction<GeneralValue> secondFunction) {
		logger.finest(this + " compose with " + secondFunction);
		EdgeFunction<GeneralValue> tmpnext;
		if (next != null) {
			tmpnext = next.composeWith(secondFunction);
			if (next == tmpnext) return this;
		}
		else tmpnext = secondFunction;

		if (tmpnext instanceof AllBottom) {
			return tmpnext;
		}
		if (tmpnext instanceof ConstantEdge) {
			return tmpnext;
		}
		EdgeFunctionTemplate tmp = this.copy();
		if (tmpnext instanceof EdgeIdentity) {
			tmp.next = null;
			return tmp;
		}
		tmp.next = tmpnext;
		return tmp.composeWithNext();
	}
	
	/**
	 * The implementation of composeWith
	 * The default implementation is just connect two edge
	 * Merge to one edge if needed
	 */
	protected EdgeFunction<GeneralValue> composeWithNext(){
		return this;
	}
	
	/**
	 * Join two edge list if two edge have same next
	 * subclass should implement joinWithFirstEdge 
	 */
	@Override
	final public EdgeFunction<GeneralValue> joinWith(EdgeFunction<GeneralValue> otherFunction){
		logger.finest(this+" join with "+otherFunction);
		if (otherFunction instanceof EdgeFunctionTemplate) {
			EdgeFunctionTemplate otherTemplate = (EdgeFunctionTemplate) otherFunction;
			if (next == null && otherTemplate.next == null) return joinWithFirstEdge(otherFunction);
			if (next != null && otherTemplate.next != null && next.equalTo(otherTemplate.next)) return joinWithFirstEdge(otherFunction);
		}
		else if (next == null) {
			return joinWithFirstEdge(otherFunction);
		}
		return new AllBottom<GeneralValue>(BottomValue.v());
	}
	
	abstract public EdgeFunction<GeneralValue> joinWithFirstEdge(EdgeFunction<GeneralValue> otherFunction);
	
	/** 
	 * check the equality of edge list
	 * subclass should implement its own equalToFirst to compare the first edge
	 */
	@Override
	final public boolean equalTo(EdgeFunction<GeneralValue> other) {
		if (other instanceof EdgeFunctionTemplate) {
			EdgeFunctionTemplate otherTemplate = (EdgeFunctionTemplate) other;
			if (next == null && otherTemplate.next == null) return equalToFirstEdge(other);
			if (next != null && otherTemplate.next != null) return next.equalTo(otherTemplate.next) && equalToFirstEdge(other);
		}
		return false;
	}

	/** 
	 * check the equality of the first edge in the edge list
	 */
	abstract protected boolean equalToFirstEdge(EdgeFunction<GeneralValue> other);

	/**
	 * The method is a wrapper of edgeToString to handle edge list
	 * subclass should implement its own toString as edgeToString
	 */
	@Override
	final public String toString(){
		if (next == null) return edgeToString();
		else return edgeToString() + "+" + next.toString();
	}
	
	abstract public String edgeToString();
}
