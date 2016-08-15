package hisdroid.instrumenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import hisdroid.Analyzer;
import hisdroid.TriLogic;
import hisdroid.value.BottomValue;
import hisdroid.value.CmpValue;
import hisdroid.value.DataValue;
import hisdroid.value.EqualityValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.NullValue;
import hisdroid.value.PrimitiveDataValue;
import hisdroid.value.StringValue;
import hisdroid.value.interfaces.ComparableGeneralValue;
import hisdroid.value.interfaces.EqualableGeneralValue;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LeExpr;
import soot.jimple.LongConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.util.queue.QueueReader;

abstract public class AnalysisInstrumenter {
	static protected final Logger logger = Logger.getLogger("HisDroid");
	Analyzer analyzer;
	
	public AnalysisInstrumenter(Analyzer analyzer){
		this.analyzer = analyzer;
	}
	
	public void instrument(){
		logger.info("Start instrument");
		for (QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();qr.hasNext(); ) {
			SootMethod s = qr.next().method();
			if (shouldInstrument(s)) {
				logger.fine("Instrument "+s.toString());
				instrument(s);
			}
		}
		logger.info("End instrument");
	}
	
	public static boolean shouldInstrument(SootMethod s) {
		String packageName = s.getDeclaringClass().getJavaPackageName();
		return s.getDeclaringClass().isApplicationClass() &&
				s.isConcrete() &&
				!packageName.startsWith("android.") &&
				!packageName.startsWith("com.android.") &&
				!packageName.startsWith("hisdroid.");
	}
	
	public Analyzer getAnalyzer() {
		return analyzer;
	}
	
	void instrument(SootMethod m) {
		PatchingChain<Unit> unitChain = m.getActiveBody().getUnits();
		
		List<IfStmt> ifList = new ArrayList<IfStmt>();
		List<SwitchStmt> switchList = new ArrayList<SwitchStmt>();
		
		for (Unit u: unitChain) {
			Map<Value, GeneralValue> map = analyzer.resultsAt(u);
			for (Value key : map.keySet()) {
				logger.finest("\t"+key + ": " + map.get(key));
			}
			logger.finest(u.toString());
			if (u instanceof IfStmt) ifList.add((IfStmt)u);
			else if (u instanceof SwitchStmt) switchList.add((SwitchStmt)u);
		}

		for (SwitchStmt ss: switchList) {
			ResultOfSwitch result = resultAtSwitch(ss);
			if (result.isBottom()) {
				logger.fine(String.format("Unknown at Switch: %s", ss));
			}
			else {
				List<Unit> newUnits = instrumentSwitch(unitChain, ss, result);
				logger.fine(String.format("Change Switch: %s to %s", ss, newUnits));
			}
		}
		for (IfStmt is: ifList) {
			TriLogic result = resultAtBranch(is);
			logger.fine(String.format("%s at branch: %s", result, is));
			if (result != TriLogic.Unknown) {
				instrumentBranch(unitChain, is, result);
			}
		}
	}

	abstract void instrumentBranch(PatchingChain<Unit> unitChain, IfStmt stmt, TriLogic result);
	abstract public List<Unit> instrumentSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt, ResultOfSwitch result);

	public TriLogic resultAtBranch(IfStmt stmt){
		Map<Value, GeneralValue> map = analyzer.resultsAt(stmt);
		Value condition = stmt.getCondition();
		
		if (condition instanceof Local || condition instanceof FieldRef){
			if (map.containsKey(condition)) {
				GeneralValue gv = map.get(condition);
				if (gv instanceof EqualityValue) return equalityValueToTriLogic((EqualityValue)gv);
				if (gv instanceof CmpValue) gv = cmpValueToIntValue((CmpValue)gv);
				return gv.triLogic();
			}
		}
		else if (condition instanceof NegExpr) {
			Value op = ((NegExpr)condition).getOp();
			if (op instanceof Local || op instanceof FieldRef){
				if (map.containsKey(op)) {
					GeneralValue gv = map.get(condition);
					if (gv instanceof EqualityValue) return equalityValueToTriLogic((EqualityValue)gv).not();
					if (gv instanceof CmpValue) gv = cmpValueToIntValue((CmpValue)gv);
					return gv.triLogic().not();
				}
			}
		}
		else if (condition instanceof ConditionExpr) {
			ConditionExpr conditionExpr = (ConditionExpr) condition;
			Value op1 = conditionExpr.getOp1();
			Value op2 = conditionExpr.getOp2();
			
			// Get GeneralValue from op
			GeneralValue gv1 = null, gv2 = null;
			if (map.containsKey(op1)) gv1 = map.get(op1);
			if (map.containsKey(op2)) gv2 = map.get(op2);
			
			// Test null
			if (condition instanceof EqExpr) {
				if (op1 instanceof NullConstant) return isNullValue(gv2);
				else if (op2 instanceof NullConstant) return isNullValue(gv1);
			}
			if (condition instanceof NeExpr) {
				if (op1 instanceof NullConstant) return isNullValue(gv2).not();
				else if (op2 instanceof NullConstant) return isNullValue(gv1).not();
			}
			// if op is a constant
			if (op1 instanceof Constant) {
				gv1 = constantToGeneralValue((Constant)op1);
			}
			if (op2 instanceof Constant) {
				gv2 = constantToGeneralValue((Constant)op2);
			}
			// if gv is cmp value
			if (gv1 instanceof CmpValue) {
				gv1 = cmpValueToIntValue((CmpValue)gv1);
			}
			if (gv2 instanceof CmpValue) {
				gv2 = cmpValueToIntValue((CmpValue)gv2);
			}
			// if gv is eq value
			if (gv1 instanceof EqualityValue) {
				gv1 = equalityValueToIntValue((EqualityValue)gv1);
			}
			if (gv2 instanceof EqualityValue) {
				gv2 = equalityValueToIntValue((EqualityValue)gv2);
			}

			PrimitiveDataValue<Integer> iv1 = castGeneralValueToIntValue(gv1);
			PrimitiveDataValue<Integer> iv2 = castGeneralValueToIntValue(gv2);
			
			logger.finest(String.format("Cmp %s and %s at %s", iv1, iv2, stmt));
			
			// return unknown if not IntValue
			if (iv1 == null || iv2 == null) return TriLogic.Unknown;
			
			// return unknown if bottom
			if (iv1.bottom() || iv2.bottom()) return TriLogic.Unknown;
			
			// Test value
			if (condition instanceof EqExpr) {
				// if iv1 and iv2 only have one possible value, check the value equality 
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(iv1.valueSet().iterator().next().equals(iv2.valueSet().iterator().next()));
				}
				// if two value has empty intersection, return false
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return TriLogic.False;
			}
			else if (condition instanceof NeExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(!iv1.valueSet().iterator().next().equals(iv2.valueSet().iterator().next()));
				}
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return TriLogic.True;
			}
			else if (condition instanceof GeExpr) {
				// if (all possible value in iv1) >= (all possible value in iv2) return true
				if (Collections.min(iv1.valueSet()) >= Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) < Collections.min(iv2.valueSet())) return TriLogic.False;
			}
			else if (condition instanceof GtExpr) {
				if (Collections.min(iv1.valueSet()) > Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) <= Collections.min(iv2.valueSet())) return TriLogic.False;
			}
			else if (condition instanceof LeExpr) {
				if (Collections.max(iv1.valueSet()) <= Collections.min(iv2.valueSet())) return TriLogic.True;
				if (Collections.min(iv1.valueSet()) > Collections.max(iv2.valueSet())) return TriLogic.False;
			}
			else if (condition instanceof LtExpr) {
				if (Collections.max(iv1.valueSet()) < Collections.min(iv2.valueSet())) return TriLogic.True;
				if (Collections.min(iv1.valueSet()) >= Collections.max(iv2.valueSet())) return TriLogic.False;
			}
		}
		return TriLogic.Unknown;
	}
	
	public class ResultOfSwitch {
		boolean bottom;
		Map<Integer, Unit> reachableValueToTarget;
		boolean defaultTargetIsReachable;

		public ResultOfSwitch() {
			bottom = true;
		}
		
		public ResultOfSwitch(Map<Integer, Unit> reachableValueToTarget, boolean defaultTargetIsReachable) {
			bottom = false;
			this.reachableValueToTarget = reachableValueToTarget;
			this.defaultTargetIsReachable = defaultTargetIsReachable;
		}
		
		public boolean isBottom() { return bottom; }
		public Map<Integer, Unit> getReachableValueToTarget() { return reachableValueToTarget; }
		public boolean getDefaultTargetIsReachable() { return defaultTargetIsReachable; }
	}
	
	public ResultOfSwitch resultAtSwitch(SwitchStmt stmt){
		Map<Value, GeneralValue> map = analyzer.resultsAt(stmt);
		Value key = stmt.getKey();
		
		GeneralValue gv = map.get(key);
		PrimitiveDataValue<Integer> iv = castGeneralValueToIntValue(gv);

		// return if iv is unknown
		if (iv == null || iv.bottom()) return new ResultOfSwitch();

		// reachable targets
		Map<Integer, Unit> reachableValueToTarget = new HashMap<Integer, Unit>();
		boolean defaultTargetIsReachable = false;
		
		if (stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt s = (LookupSwitchStmt) stmt;
			// lookupValue -> target index
			Map<Integer, Integer> lookupValueToIndex = new HashMap<Integer, Integer>();
			for (int i=0; i<s.getTargetCount(); i++) {
				lookupValueToIndex.put(s.getLookupValue(i), i);
			}
			
			// collect reachable targets
			for (Integer i: iv.valueSet()) {
				if (lookupValueToIndex.containsKey(i)) {
					if (!reachableValueToTarget.containsKey(i)) {
						reachableValueToTarget.put(i, s.getTarget(lookupValueToIndex.get(i)));
					}
				}
				else defaultTargetIsReachable = true;
			}
			// if all path are reachable
			if (s.getTargetCount()==reachableValueToTarget.size() && defaultTargetIsReachable) {
				return new ResultOfSwitch();
			}
		}
		else if (stmt instanceof TableSwitchStmt) {
			TableSwitchStmt s = (TableSwitchStmt) stmt;
			int high = s.getHighIndex();
			int low = s.getLowIndex();
			
			for (Integer i: iv.valueSet()) {
				if (i >= low && i <= high) {
					if (!reachableValueToTarget.containsKey(i)) {
						reachableValueToTarget.put(i, s.getTarget(i-low));
					}
				}
				else defaultTargetIsReachable = true;
			}
			// if all path are reachable
			if (high-low+1==reachableValueToTarget.size() && defaultTargetIsReachable) {
				return new ResultOfSwitch();
			}
		}
		return new ResultOfSwitch(reachableValueToTarget, defaultTargetIsReachable);
	}
	
	@SuppressWarnings("unchecked")
	PrimitiveDataValue<Integer> castGeneralValueToIntValue(GeneralValue v){
		if (v instanceof PrimitiveDataValue) {
			PrimitiveDataValue<?> pdv = (PrimitiveDataValue<?>) v;
			if (pdv.type()==Integer.class) {
				return (PrimitiveDataValue<Integer>) pdv;
			}
		}
		return null;
	}
	
	TriLogic isNullValue(GeneralValue v){
		return TriLogic.Unknown;
		// disable null value test
		/*if (v instanceof NullValue) return TriLogic.True;
		else if (v instanceof BottomValue || v == null) return TriLogic.Unknown;
		else if (v instanceof DataValue) {
			DataValue<?> dv = (DataValue<?>) v;
			if (dv.bottom()) return TriLogic.Unknown;
		}
		return TriLogic.False;*/
	}
	
	PrimitiveDataValue<Integer> cmpValueToIntValue(CmpValue cv) {
		Map<Value, GeneralValue> map = analyzer.resultsAt(cv.unit());
		GeneralValue lhs = map.get(cv.lhs());
		GeneralValue rhs = map.get(cv.rhs());
		
		if (cv.lhs() instanceof Constant) {
			lhs = constantToGeneralValue((Constant) cv.lhs());
		}
		if (cv.rhs() instanceof Constant) {
			rhs = constantToGeneralValue((Constant) cv.rhs());
		}
		
		if (lhs instanceof ComparableGeneralValue && rhs instanceof ComparableGeneralValue) {
			ComparableGeneralValue clhs = (ComparableGeneralValue) lhs;
			ComparableGeneralValue crhs = (ComparableGeneralValue) rhs;
			return new PrimitiveDataValue<Integer>(Integer.class, clhs.cmpTo(crhs));
		}
		return new PrimitiveDataValue<Integer>(Integer.class);
	}
	
	PrimitiveDataValue<Integer> equalityValueToIntValue(EqualityValue ev) {
		switch (equalityValueToTriLogic(ev)) {
		case True:
			return new PrimitiveDataValue<Integer>(Integer.class, Collections.singleton(1));
		case False:
			return new PrimitiveDataValue<Integer>(Integer.class, Collections.singleton(0));
		case Unknown:
		default:
			Set<Integer> s = new HashSet<Integer>();
			s.add(0);
			s.add(1);
			return new PrimitiveDataValue<Integer>(Integer.class, s);
		}
	}
	
	TriLogic equalityValueToTriLogic(EqualityValue ev) {
		Map<Value, GeneralValue> map = analyzer.resultsAt(ev.unit());
		GeneralValue lhs = map.get(ev.lhs());
		GeneralValue rhs = map.get(ev.rhs());
		
		if (ev.lhs() instanceof Constant) {
			lhs = constantToGeneralValue((Constant) ev.lhs());
		}
		if (ev.rhs() instanceof Constant) {
			rhs = constantToGeneralValue((Constant) ev.rhs());
		}
		
		if (lhs instanceof EqualableGeneralValue && rhs instanceof EqualableGeneralValue) {
			EqualableGeneralValue elhs = (EqualableGeneralValue) lhs;
			EqualableGeneralValue erhs = (EqualableGeneralValue) rhs;
			return elhs.eqTo(erhs);
		}
		
		return TriLogic.Unknown;
	}
	
	GeneralValue constantToGeneralValue(Constant c){
		if (c instanceof IntConstant) {
			return new PrimitiveDataValue<Integer>(Integer.class, ((IntConstant)c).value);
		}
		if (c instanceof LongConstant) {
			return new PrimitiveDataValue<Long>(Long.class, ((LongConstant)c).value);
		}
		if (c instanceof FloatConstant) {
			return new PrimitiveDataValue<Float>(Float.class, ((FloatConstant)c).value);
		}
		if (c instanceof DoubleConstant) {
			return new PrimitiveDataValue<Double>(Double.class, ((DoubleConstant)c).value);
		}
		if (c instanceof StringConstant) {
			return new StringValue(((StringConstant)c).value);
		}
		if (c instanceof NullConstant) {
			return new NullValue();
		}
		return BottomValue.v();
	}
}
