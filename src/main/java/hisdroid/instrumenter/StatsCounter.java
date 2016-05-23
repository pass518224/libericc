package hisdroid.instrumenter;

public class StatsCounter {
	static public void branchSuccess(int branchId) {
		System.out.println("hisdroid: branch success at branch "+branchId);
	}
	
	static public void branchFail(int branchId) {
		System.out.println("hisdroid: branch failed at branch "+branchId);
	}
}
