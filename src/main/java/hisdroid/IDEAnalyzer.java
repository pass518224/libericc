package hisdroid;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import hisdroid.ide.IDEProblem;
import hisdroid.value.BottomValue;
import hisdroid.value.CmpValue;
import hisdroid.value.EqualityValue;
import hisdroid.value.GeneralValue;
import hisdroid.value.NullValue;
import hisdroid.value.PrimitiveDataValue;
import hisdroid.value.StringValue;
import hisdroid.value.interfaces.ComparableGeneralValue;
import hisdroid.value.interfaces.EqualableGeneralValue;
import soot.Local;
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
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class IDEAnalyzer implements Analyzer{
	static final Logger logger = Logger.getLogger("IDELog");
	
	DefaultJimpleIDETabulationProblem<Value, GeneralValue, InterproceduralCFG<Unit,SootMethod>> problem;
	IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>> solver;
	
	public IDEAnalyzer(){}
	
	@Override
	public void analyze(SootMethod mainMethod) {
		logger.info("IDE analysis starts");
		problem = new IDEProblem(new JimpleBasedInterproceduralCFG(), mainMethod);
		solver = new IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>>(problem);
		solver.solve();
		logger.info("IDE analysis ends");
	}
	
	@Override
	public Map<Value, GeneralValue> resultsAt(Unit stmt){
		return solver.resultsAt(stmt);
	}
	
	@Override
	public TriLogic branchAt(IfStmt stmt){
		Map<Value, GeneralValue> map = solver.resultsAt(stmt);
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
			if ((op1 instanceof NullConstant || gv1 instanceof NullValue) && (op2 instanceof NullConstant || gv2 instanceof NullValue)) {
				return TriLogic.True;
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

			// Check whether it is integer value
			if (gv1 instanceof PrimitiveDataValue && gv2 instanceof PrimitiveDataValue) {
				PrimitiveDataValue<?> pdv1 = (PrimitiveDataValue<?>) gv1;
				PrimitiveDataValue<?> pdv2 = (PrimitiveDataValue<?>) gv2;
				if (pdv1.type() == Integer.class && pdv2.type() == Integer.class) {
					@SuppressWarnings("unchecked")
					PrimitiveDataValue<Integer> iv1 = (PrimitiveDataValue<Integer>) pdv1;
					@SuppressWarnings("unchecked")
					PrimitiveDataValue<Integer> iv2 = (PrimitiveDataValue<Integer>) pdv2;
					
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
						if (Collections.min(iv1.valueSet()) <= Collections.max(iv2.valueSet())) return TriLogic.True;
						if (Collections.max(iv1.valueSet()) > Collections.min(iv2.valueSet())) return TriLogic.False;
					}
					else if (condition instanceof LtExpr) {
						if (Collections.min(iv1.valueSet()) < Collections.max(iv2.valueSet())) return TriLogic.True;
						if (Collections.max(iv1.valueSet()) >= Collections.min(iv2.valueSet())) return TriLogic.False;
					}
				}
			}
		}
		return TriLogic.Unknown;
	}
	
	PrimitiveDataValue<Integer> cmpValueToIntValue(CmpValue cv) {
		Map<Value, GeneralValue> map = solver.resultsAt(cv.unit());
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
		Map<Value, GeneralValue> map = solver.resultsAt(ev.unit());
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
