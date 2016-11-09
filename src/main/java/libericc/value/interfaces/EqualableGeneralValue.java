package libericc.value.interfaces;

import libericc.PredictResult;

public interface EqualableGeneralValue {
	PredictResult eqTo(EqualableGeneralValue rhs);
}
