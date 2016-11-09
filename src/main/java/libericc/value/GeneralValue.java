package libericc.value;

import libericc.TriLogic;

public abstract class GeneralValue {
	public abstract GeneralValue joinWith(GeneralValue otherValue);
	public TriLogic triLogic() {
		return TriLogic.Unknown;
	}
}
