package libericc;

/*
 * Representation of the predict result of analysis
 * True: The true branch is taken
 * False: The false branch is taken
 * Both: Both branch are taken
 * Ambiguous: Not able to decide the branch result(ex. the condition variable is unknown in IDE analysis)
 * Unknown: Not in the analysis scope (ex. the branch is not in call graph, the condition variable is not in IDE analysis)
 */

public enum PredictResult {
	True, False, Both, Ambiguous, Unknown;
	
	static public PredictResult toPredictResult(boolean b) {
		if (b) return True;
		else return False;
	}
	
	static public PredictResult toPredictResult(TriLogic l) {
		switch (l) {
		case True:
			return True;
		case False:
			return False;
		case Unknown:
		default:
			return Ambiguous;
		}
	}
	
	static public PredictResult parseString(String str) {
		if ("True".equals(str)) return True;
		else if ("False".equals(str)) return False;
		else if ("Both".equals(str)) return Both;
		else if ("Ambiguous".equals(str)) return Ambiguous;
		else return Unknown;
	}
	
	public TriLogic toTriLogic() {
		switch (this) {
		case True:
			return TriLogic.True;
		case False:
			return TriLogic.False;
		default:
			return TriLogic.Unknown;
		}
	}

	public boolean isTrue() {
		return this == True;
	}
	
	public boolean isFalse() {
		return this == False;
	}

	public boolean isBoth() {
		return this == Both;
	}
	
	public boolean isAmbiguous() {
		return this == Ambiguous;
	}
	
	public boolean isUnknown() {
		return this == Unknown;
	}
	
	public PredictResult not() {
		switch (this) {
		case True:
			return False;
		case False:
			return True;
		default:
			return this;
		}
	}
}
