package hisdroid;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import hisdroid.ide.IDEProblem;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValue;
import hisdroid.value.NullValue;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.FieldRef;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.NullConstant;
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
				return map.get(condition).triLogic();
			}
		}
		if (condition instanceof NegExpr) {
			Value op = ((NegExpr)condition).getOp();
			if (op instanceof Local || op instanceof FieldRef){
				if (map.containsKey(op)) {
					return map.get(op).triLogic().not();
				}
			}
		}
		if (condition instanceof ConditionExpr) {
			ConditionExpr conditionExpr = (ConditionExpr) condition;
			Value op1 = conditionExpr.getOp1();
			Value op2 = conditionExpr.getOp2();
			// Test null
			if ((op1 instanceof NullConstant || op1 instanceof NullValue) && (op2 instanceof NullConstant || op2 instanceof NullValue)) {
				return TriLogic.True;
			}
			// Test int
			IntValue iv1 = null, iv2 = null;
			// Get int value from op1
			if (map.containsKey(op1)) {
				GeneralValue v1 = map.get(op1);
				if (v1 instanceof IntValue) {
					iv1 = (IntValue) v1;
				}
				else return TriLogic.Unknown;
			}
			else if (op1 instanceof IntConstant) {
				iv1 = new IntValue(((IntConstant) op1).value);
			}
			else return TriLogic.Unknown;
			// Get int value from op2
			if (map.containsKey(op2)) {
				GeneralValue v2 = map.get(op2);
				if (v2 instanceof IntValue) {
					iv1 = (IntValue) v2;
				}
				else return TriLogic.Unknown;
			}
			else if (op2 instanceof IntConstant) {
				iv2 = new IntValue(((IntConstant) op2).value);
			}
			else return TriLogic.Unknown;

			// return unknown if bottom
			if (iv1.bottom() || iv2.bottom()) return TriLogic.Unknown;
			// Test value
			if (condition instanceof EqExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(iv1.valueSet().iterator().next() == iv2.valueSet().iterator().next());
				}
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return TriLogic.False;
				return TriLogic.Unknown;
			}
			if (condition instanceof NeExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(iv1.valueSet().iterator().next() != iv2.valueSet().iterator().next());
				}
				Set<Integer> intersection = new HashSet<Integer>(iv1.valueSet());
				intersection.retainAll(iv2.valueSet());
				if (intersection.isEmpty()) return TriLogic.True;
				return TriLogic.Unknown;
			}
			if (condition instanceof GeExpr) {
				if (Collections.min(iv1.valueSet()) >= Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) < Collections.min(iv2.valueSet())) return TriLogic.False;
				return TriLogic.Unknown;
			}
			if (condition instanceof GtExpr) {
				if (Collections.min(iv1.valueSet()) > Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) <= Collections.min(iv2.valueSet())) return TriLogic.False;
				return TriLogic.Unknown;
			}
			if (condition instanceof LeExpr) {
				if (Collections.min(iv1.valueSet()) <= Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) > Collections.min(iv2.valueSet())) return TriLogic.False;
				return TriLogic.Unknown;
			}
			if (condition instanceof LtExpr) {
				if (Collections.min(iv1.valueSet()) < Collections.max(iv2.valueSet())) return TriLogic.True;
				if (Collections.max(iv1.valueSet()) >= Collections.min(iv2.valueSet())) return TriLogic.False;
				return TriLogic.Unknown;
			}
		}
		return TriLogic.Unknown;
	}
	
}
