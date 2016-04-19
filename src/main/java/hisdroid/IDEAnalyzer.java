package hisdroid;

import java.util.Map;
import java.util.logging.Logger;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import hisdroid.ide.IDEProblem;
import hisdroid.value.GeneralValue;
import hisdroid.value.IntValue;
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
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class IDEAnalyzer implements Analyzer{
	static final Logger logger = Logger.getLogger("IDELog");
	
	DefaultJimpleIDETabulationProblem<Value, GeneralValue, InterproceduralCFG<Unit,SootMethod>> problem;
	IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>> solver;
	
	public IDEAnalyzer(){}
	
	@Override
	public void analyze(SootMethod mainMethod) {
		problem = new IDEProblem(new JimpleBasedInterproceduralCFG(), mainMethod);
		solver = new IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>>(problem);
		solver.solve();
	}
	
	@Override
	public Map<Value, GeneralValue> resultsAt(Unit stmt){
		return solver.resultsAt(stmt);
	}
	
	@Override
	public TriLogic branchAt(IfStmt stmt){
		Map<Value, GeneralValue> map = solver.resultsAt(stmt);
		IfStmt ifs = (IfStmt) stmt;
		Value condition = ifs.getCondition();
		
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
			IntValue iv1 = null, iv2 = null;
			
			if (map.containsKey(op1)) {
				GeneralValue v1 = map.get(op1);
				if (v1 instanceof IntValue) {
					iv1 = (IntValue) v1;
				}
				else return TriLogic.Unknown;
			}
			else if (op1 instanceof IntConstant) {
				IntConstant ic1 = (IntConstant) op1; 
				iv1 = new IntValue(ic1.value);
			}
			else return TriLogic.Unknown;
			
			if (map.containsKey(op2)) {
				GeneralValue v2 = map.get(op2);
				if (v2 instanceof IntValue) {
					iv1 = (IntValue) v2;
				}
				else return TriLogic.Unknown;
			}
			else if (op2 instanceof IntConstant) {
				IntConstant ic2 = (IntConstant) op2; 
				iv2 = new IntValue(ic2.value);
			}
			else return TriLogic.Unknown;

			if (iv1.bottom() || iv2.bottom()) return TriLogic.Unknown;
			if (condition instanceof EqExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(iv1.valueSet().iterator().next() == iv2.valueSet().iterator().next()); 
				}
				return TriLogic.Unknown;
			}
			if (condition instanceof GeExpr) {
				boolean allTrue = true, allFalse = true;
				for (int i: iv1.valueSet()) {
					for (int j: iv2.valueSet()) {
						if (i>=j) {
							allFalse = false;
						}
						else {
							allTrue = false;
						}
						if (!allTrue && !allFalse) return TriLogic.Unknown;
					}
				}
				if (allTrue && !allFalse) return TriLogic.True;
				if (!allTrue && allFalse) return TriLogic.False;
			}
			if (condition instanceof GtExpr) {
				boolean allTrue = true, allFalse = true;
				for (int i: iv1.valueSet()) {
					for (int j: iv2.valueSet()) {
						if (i>j) {
							allFalse = false;
						}
						else {
							allTrue = false;
						}
						if (!allTrue && !allFalse) return TriLogic.Unknown;
					}
				}
				if (allTrue && !allFalse) return TriLogic.True;
				if (!allTrue && allFalse) return TriLogic.False;
			}
			if (condition instanceof LeExpr) {
				boolean allTrue = true, allFalse = true;
				for (int i: iv1.valueSet()) {
					for (int j: iv2.valueSet()) {
						if (i<=j) {
							allFalse = false;
						}
						else {
							allTrue = false;
						}
						if (!allTrue && !allFalse) return TriLogic.Unknown;
					}
				}
				if (allTrue && !allFalse) return TriLogic.True;
				if (!allTrue && allFalse) return TriLogic.False;
			}
			if (condition instanceof LtExpr) {
				boolean allTrue = true, allFalse = true;
				for (int i: iv1.valueSet()) {
					for (int j: iv2.valueSet()) {
						if (i<j) {
							allFalse = false;
						}
						else {
							allTrue = false;
						}
						if (!allTrue && !allFalse) return TriLogic.Unknown;
					}
				}
				if (allTrue && !allFalse) return TriLogic.True;
				if (!allTrue && allFalse) return TriLogic.False;
			}
			if (condition instanceof NeExpr) {
				if (iv1.valueSet().size() == 1 && iv2.valueSet().size() == 1){
					return TriLogic.toTriLogic(iv1.valueSet().iterator().next() != iv2.valueSet().iterator().next()); 
				}
				return TriLogic.Unknown;
			}
		}
		return TriLogic.Unknown;
	}
}
