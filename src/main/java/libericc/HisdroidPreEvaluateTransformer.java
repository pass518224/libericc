package libericc;

import java.util.Map;

import libericc.preevaluate.PreEvaluateInstrumenter;
import soot.SceneTransformer;

/*
 * A SceneTransformer to insert logging code for evaluation
 */

class HisdroidPreEvaluateTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		PreEvaluateInstrumenter instrumenter = new PreEvaluateInstrumenter();
		instrumenter.instrument();
	}
}
