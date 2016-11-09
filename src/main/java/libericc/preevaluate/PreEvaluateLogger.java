package libericc.preevaluate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PreEvaluateLogger {
	static Map<Long, Integer> idResult2Count = new ConcurrentHashMap<Long, Integer>();
	static Map<Integer, Integer> method2Count = new ConcurrentHashMap<Integer, Integer>();
	static long branchCount = 0;
	static long threshold = 10000;
	
	public static void branchResult(int branchId, int value){
		Long iv = ((long)branchId)<<32|value;
		//System.out.println(String.format("B: %d=%d", idResult, value));
		Integer count = idResult2Count.get(iv);
		idResult2Count.put(iv, count==null? 1: count+1);
		branchCount++;
		if (branchCount%threshold==0) {
			dump();
		}
	}
	
	public static void methodInvoke(int methodHash) {
		Integer count = method2Count.get(methodHash);
		method2Count.put(methodHash, count==null? 1: count+1);
		branchCount++;
		if (branchCount%threshold==0) {
			dump();
		}
	}

	static void dump() {
		System.out.println("Total Branch Count: "+branchCount);
		for (Map.Entry<Long, Integer> entry: idResult2Count.entrySet()) {
			System.out.println(String.format("libericc: B: %d:%d:%d", entry.getKey()>>32, entry.getKey().intValue(), entry.getValue()));
		}
		for (Map.Entry<Integer, Integer> entry: method2Count.entrySet()) {
			System.out.println(String.format("libericc: M: %d:%d", entry.getKey().intValue(), entry.getValue()));
		}
		idResult2Count.clear();
		method2Count.clear();
	}
}