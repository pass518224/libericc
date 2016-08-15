package hisdroid.instrumenter.pre;

import android.content.Intent;
import android.os.Bundle;

public class PreEvaluateLogger {
	
	public static void methodInvoke(String method, String args, int hash) {
		System.out.println(String.format("hisdroid: M: %s#%d=(%s)", method, hash, args));
	}
	
	public static void branchResult(String method, int branchId, int value, int hash){
		System.out.println(String.format("hisdroid: B: %s:%d#%d=%d", method, branchId, hash, value));
	}
	
	public static String intentToString(Intent i) {
		if (i!=null) {
			return i.toString() + "BundleStart" + bundleToString(i.getExtras()) + "BundleEnd";
		}
		else return "null";
	}
	
	public static String bundleToString(Bundle b) {
		if (b!=null) {
			String s = "";
			for (String key: b.keySet()) {
				s += String.format("%s->%s[BundleDelimeter]", key, b.get(key));
			}
			return s;
		}
		else return "null";
	}
}
