package hisdroid;

import java.util.Map;

import hisdroid.instrumenter.PreEvaluateInstrumenter;
import soot.SceneTransformer;

class HisdroidPreEvaluateTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		PreEvaluateInstrumenter instrumenter = new PreEvaluateInstrumenter();
		instrumenter.instrument();
	}
}