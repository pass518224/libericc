package libericc;

import java.util.Map;

import libericc.Config.Instrument;
import libericc.decider.AnalysisDecider;
import libericc.decider.Decider;
import libericc.evaluation.AggressiveEvaluator;
import libericc.evaluation.Evaluator;
import libericc.evaluation.NormalEvaluator;
import libericc.evaluation.SeperatedEvaluator;
import libericc.instrumenter.AnalysisInstrumenter;
import libericc.instrumenter.Instrumenter;
import libericc.pruner.AggressiveBranchPruner;
import libericc.pruner.BranchPruner;
import libericc.pruner.ConservativeBranchPruner;
import libericc.pruner.DoNothingBranchPruner;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.options.Options;

class HisdroidAnalysisTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		Evaluator evaluator = null;
		Analyzer analyzer = new IDEAnalyzer();
		Instrumenter instrumenter;

		// Create dummy main method and build call graph
		new DummyMainCreator().createDummyMain();
		Options.v().setPhaseOption("cg", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
		PackManager.v().getPack("cg").apply();
		
		// Initialize evaluator
		if (Config.adblogPath != null) {
			if (Config.iccNo==0) {
				if (Config.instrument == Instrument.aggressive) {
					evaluator = new AggressiveEvaluator();
				}
				else {
					evaluator = new NormalEvaluator();
				}
			}
			else {
				evaluator = new SeperatedEvaluator(analyzer);
			}
		}
		
		// new decider
		Decider decider = new AnalysisDecider(analyzer);

		// new pruner based on option
		BranchPruner pruner = null;
		switch (Config.instrument) {
		case none:
			pruner = new DoNothingBranchPruner();
			break;
		default:
		case prune:
			pruner = new ConservativeBranchPruner();
			break;
		case aggressive:
			pruner = new AggressiveBranchPruner();
			break;
		}
		instrumenter = new AnalysisInstrumenter(decider, pruner);
		
		// start analysis
		analyzer.analyze(Scene.v().getMainMethod());

		// evaluate
		if (Config.adblogPath != null) {
			evaluator.evaluate(decider);
		}
		// instrument
		instrumenter.instrument();
	}
}