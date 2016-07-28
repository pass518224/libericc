package hisdroid;

import java.util.Map;
import hisdroid.instrumenter.AnalysisInstrumenter;
import hisdroid.instrumenter.LogOnly;
import hisdroid.instrumenter.Pruner;
import hisdroid.instrumenter.StatsInstrumenter;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.options.Options;

class HisdroidAnalysisTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		Evaluator evaluator = null;

		new DummyMainCreator().createDummyMain();
		Options.v().setPhaseOption("cg", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
		PackManager.v().getPack("cg").apply();
		
		if (Config.adblogPath != null) {
			evaluator = new Evaluator();
		}
		
		Analyzer analyzer = new IDEAnalyzer();
		AnalysisInstrumenter instrumenter;
		switch(Config.instrument) {
		case none:
			instrumenter = new LogOnly(analyzer);
			break;
		default:
		case prune:
			instrumenter = new Pruner(analyzer);
			break;
		case stats:
			instrumenter = new StatsInstrumenter(analyzer);
		}
		analyzer.analyze(Scene.v().getMainMethod());

		if (Config.adblogPath != null) {
			evaluator.evaluate(instrumenter);
		}
		instrumenter.instrument();
	}
}