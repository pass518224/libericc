package libericc.relation;

import java.util.Map;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import libericc.Logger;
import libericc.relation.decider.ICCRelationDecider;
import libericc.relation.decider.ICCRelationDepthDecider;
import libericc.relation.pruner.RemovePruner;
import libericc.relation.pruner.ReturnPruner;
import soot.PackManager;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;

class AnalysisTransformer extends SceneTransformer {
	
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		new IFDSDummyMainCreator().createDummyMain();

		Options.v().setPhaseOption("cg", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
		PackManager.v().getPack("cg").apply();
		
		Logger.getLogger().info("Start IFDS analysis");
		
		DefaultJimpleIFDSTabulationProblem<Value, InterproceduralCFG<Unit,SootMethod>> problem;
		IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;
		problem = new IFDSProblem(new JimpleBasedInterproceduralCFG());
		solver = new IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>>(problem);
		solver.solve();

		Logger.getLogger().info("End IFDS analysis");
		
		InstrumenterNew instrumenter = new InstrumenterNew(new ICCRelationDepthDecider(new ICCRelationDecider(solver), Config.depth), new RemovePruner());
		instrumenter.instrument();
	}
}