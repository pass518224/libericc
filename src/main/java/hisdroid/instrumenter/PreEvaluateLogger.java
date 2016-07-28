package hisdroid.instrumenter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PreEvaluateLogger {
	/*static Map<Long, Integer> idResult2Count = new ConcurrentHashMap<Long, Integer>();
	static long branchCount = 0;
	static long threshold = 1000;*/
	
	public static void branchResult(int branchId, int value){
		long idResult = ((long)branchId)<<32|value;
		System.out.println(String.format("B: %d=%d", idResult, value));
		/*Integer count = idResult2Count.get(idResult);
		idResult2Count.put(idResult, count==null? 1: count+1);
		branchCount++;
		if (branchCount%threshold==0) {
			dump();
			idResult2Count.clear();
		}*/
	}

	/*static void dump() {
		System.out.println("Total Branch Count: "+branchCount);
		for (Map.Entry<Long, Integer> entry: idResult2Count.entrySet()) {
			//output.println(String.format("Branch Results: %d=%d", entry.getKey(), entry.getValue()));
			System.out.println(String.format("B: %d=%d", entry.getKey(), entry.getValue()));
		}
	}*/
	
}
