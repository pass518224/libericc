package hisdroid;

import java.util.Map;
import java.util.logging.Logger;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import hisdroid.ide.IDEProblem;
import hisdroid.value.GeneralValue;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class IDEAnalyzer implements Analyzer {
	static final Logger logger = Logger.getLogger("HisDroid");
	
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
}
