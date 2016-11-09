package libericc;

import java.util.Map;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import libericc.ide.ConstantPropagationProblem;
import libericc.value.GeneralValue;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class IDEAnalyzer implements Analyzer {
	DefaultJimpleIDETabulationProblem<Value, GeneralValue, InterproceduralCFG<Unit,SootMethod>> problem;
	IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>> solver;
	
	public IDEAnalyzer(){}
	
	@Override
	public void analyze(SootMethod mainMethod) {
		Logger.getLogger().info("IDE analysis starts");
		problem = new ConstantPropagationProblem(new JimpleBasedInterproceduralCFG(), mainMethod);
		solver = new IDESolver<Unit, Value, SootMethod, GeneralValue, InterproceduralCFG<Unit, SootMethod>>(problem);
		solver.solve();
		Logger.getLogger().info("IDE analysis ends");
	}
	
	@Override
	public Map<Value, GeneralValue> resultsAt(Unit stmt){
		return solver.resultsAt(stmt);
	}
}
