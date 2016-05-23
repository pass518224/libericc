package hisdroid;

import java.util.Map;

import hisdroid.instrumenter.StatsInstrumenter;
import hisdroid.instrumenter.Instrumenter;
import hisdroid.instrumenter.LogOnly;
import hisdroid.instrumenter.Pruner;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.options.Options;

class HisdroidAnalysisTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {

		new DummyMainCreator().createDummyMain();
		
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
		PackManager.v().getPack("cg").apply();
		
		Analyzer analyzer = new IDEAnalyzer();
		analyzer.analyze(Scene.v().getMainMethod());
		
		Instrumenter instrumenter;
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
		instrumenter.instrument();
	}
}