package libericc;

import java.util.Map;

import libericc.decider.RandomDecider;
import libericc.evaluation.Evaluator;
import libericc.evaluation.NormalEvaluator;
import soot.SceneTransformer;

/*
 * A SceneTransformer used for evaluation random guessing
 */

class RandomTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		Evaluator evaluator = new NormalEvaluator();
		evaluator.evaluate(new RandomDecider());
	}
}
