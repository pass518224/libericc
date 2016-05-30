package hisdroid.instrumenter;

public class PreEvaluateLogger {
	static public void switchLog(String method, int branchId, int value) {
		System.out.println(String.format("hisdroid: %d at switch %s:%d", value, method, branchId));
	}
	
	static public void branchTrueLog(String method, int branchId) {
		System.out.println(String.format("hisdroid: True at branch %s:%d", method, branchId));
	}
	
	static public void branchFalseLog(String method, int branchId) {
		System.out.println(String.format("hisdroid: False at branch %s:%d", method, branchId));
	}
}
