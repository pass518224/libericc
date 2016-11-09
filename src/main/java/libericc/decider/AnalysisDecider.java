package libericc.decider;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import libericc.Analyzer;
import libericc.Logger;
import libericc.PredictResult;
import libericc.value.BottomValue;
import libericc.value.CmpValue;
import libericc.value.EqualityValue;
import libericc.value.GeneralValue;
import libericc.value.NullValue;
import libericc.value.PrimitiveDataValue;
import libericc.value.StringValue;
import libericc.value.interfaces.ComparableGeneralValue;
import libericc.value.interfaces.EqualableGeneralValue;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
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
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;

/*
 * Decide the branch result by analyzer
 */
public class AnalysisDecider implements Decider {
	Analyzer analyzer;

	public AnalysisDecider(Analyzer analyzer){
		this.analyzer = analyzer;
	}

	public PredictResult branchPredictResult(IfStmt stmt) {
		Map<Value, GeneralValue> map = analyzer.resultsAt(stmt);
		Value condition = stmt.getCondition();
		
		if (condition instanceof ConditionExpr) {
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
			
			Logger.getLogger().finest(String.format("Cmp %s and %s at %s", iv1, iv2, stmt));
			
			// return unknown if not IntValue
			if (iv1 == null || iv2 == null) return PredictResult.Unknown;
			
			// return unknown if bottom
			if (iv1.bottom() || iv2.bottom()) return PredictResult.Ambiguous;
			
			// Test value
			if (condition instanceof EqExpr) {
				// if iv1 and iv2 only have one possible value, check the value equality 
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return PredictResult.toPredictResult(iv1.valueSet().iterator().next().equals(iv2.valueSet().iterator().next()));
				}
				// if two value has empty intersection, return false
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return PredictResult.False;
				return PredictResult.Both;
			}
			else if (condition instanceof NeExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return PredictResult.toPredictResult(!iv1.valueSet().iterator().next().equals(iv2.valueSet().iterator().next()));
				}
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return PredictResult.True;
				return PredictResult.Both;
			}
			else if (condition instanceof GeExpr) {
				// if (all possible value in iv1) >= (all possible value in iv2) return true
				if (Collections.min(iv1.valueSet()) >= Collections.max(iv2.valueSet())) return PredictResult.True;
				if (Collections.max(iv1.valueSet()) < Collections.min(iv2.valueSet())) return PredictResult.False;
				return PredictResult.Both;
			}
			else if (condition instanceof GtExpr) {
				if (Collections.min(iv1.valueSet()) > Collections.max(iv2.valueSet())) return PredictResult.True;
				if (Collections.max(iv1.valueSet()) <= Collections.min(iv2.valueSet())) return PredictResult.False;
				return PredictResult.Both;
			}
			else if (condition instanceof LeExpr) {
				if (Collections.max(iv1.valueSet()) <= Collections.min(iv2.valueSet())) return PredictResult.True;
				if (Collections.min(iv1.valueSet()) > Collections.max(iv2.valueSet())) return PredictResult.False;
				return PredictResult.Both;
			}
			else if (condition instanceof LtExpr) {
				if (Collections.max(iv1.valueSet()) < Collections.min(iv2.valueSet())) return PredictResult.True;
				if (Collections.min(iv1.valueSet()) >= Collections.max(iv2.valueSet())) return PredictResult.False;
				return PredictResult.Both;
			}
		}
		return PredictResult.Unknown;
	}
	
	@Override
	public ResultOfSwitch switchResult(SwitchStmt stmt) {
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
	
	PredictResult isNullValue(GeneralValue v){
		return PredictResult.Ambiguous;
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
		switch (equalityValueToPredictResult(ev)) {
		case True:
			return new PrimitiveDataValue<Integer>(Integer.class, Collections.singleton(1));
		case False:
			return new PrimitiveDataValue<Integer>(Integer.class, Collections.singleton(0));
		default:
			Set<Integer> s = new HashSet<Integer>();
			s.add(0);
			s.add(1);
			return new PrimitiveDataValue<Integer>(Integer.class, s);
		}
	}
	
	PredictResult equalityValueToPredictResult(EqualityValue ev) {
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
		
		return PredictResult.Ambiguous;
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
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		for (SootClass sc: Scene.v().getClasses()) {
			for (SootMethod sm: sc.getMethods()) {
				try {
					PatchingChain<Unit> unitChain = sm.getActiveBody().getUnits();
					JSONObject methodObj = new JSONObject();
					
					List<Stmt> stmtList = new ArrayList<Stmt>();
					for (Unit u: unitChain) {
						if (u instanceof IfStmt) stmtList.add((Stmt)u);
						else if (u instanceof SwitchStmt) stmtList.add((Stmt)u);
					}

					for (int i=0; i<stmtList.size(); i++) {
						if (stmtList.get(i) instanceof IfStmt) {
							IfStmt stmt = (IfStmt) stmtList.get(i);
							PredictResult result = branchPredictResult(stmt);
							methodObj.put(Integer.toString(i), result.toString());
						}
						else {
							SwitchStmt stmt = (SwitchStmt) stmtList.get(i);
							ResultOfSwitch result = switchResult(stmt);
							methodObj.put(Integer.toString(i), result.toJSONObject());
						}
					}
					
					obj.put(sm.toString(), methodObj);
				}
				catch (Exception e) {
					continue;
				}
			}
		}
		return obj;
	}

	public void dump(String filepath) {
		try {
			JSONObject obj = toJSONObject();
			PrintWriter writer;
			writer = new PrintWriter(filepath);
			writer.println(obj.toString(4));
			writer.close();
		} catch (FileNotFoundException e) {}
	}
	
}
