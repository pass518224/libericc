package libericc.evaluatecoverage;

import java.util.ArrayList;
import java.util.Map;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

class CoverageTransformer extends SceneTransformer {
	
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		new DummyMainCreator().createDummyMain();

		Options.v().setPhaseOption("cg", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
		PackManager.v().getPack("cg").apply();
		
		int totalCode=0, totalMethod=0, activity=0, service=0, intentService=0, broadcastReceiver=0, contentProvider=0, reachableMethod=0, reachableCode=0;
		activity = Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.app.Activity")).size();
		service = Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.app.Service")).size();
		intentService = Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.app.IntentService")).size();
		broadcastReceiver = Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.content.BroadcastReceiver")).size();
		contentProvider = Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.content.ContentProvider")).size();
		for (SootClass sc: new ArrayList<SootClass>(Scene.v().getClasses())) {
			for (SootMethod sm: new ArrayList<SootMethod>(sc.getMethods())) {
				if (shouldCount(sm)) {
					totalMethod++;
					boolean reachable = Scene.v().getReachableMethods().contains(sm);
					if (reachable) reachableMethod++;
					int codeSize = sm.retrieveActiveBody().getUnits().size();
					totalCode += codeSize;
					if (reachable) reachableCode += codeSize;
				}
			}
		}
		/*System.out.println(Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.app.Activity")));
		System.out.println(Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.app.Service")));
		System.out.println(Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.content.BroadcastReceiver")));
		System.out.println(Scene.v().getActiveHierarchy().getSubclassesOf(Scene.v().getSootClass("android.content.ContentProvider")));*/
		System.out.println("totalCode: "+totalCode);
		System.out.println("totalMethod: "+totalMethod);
		System.out.println("activity: "+activity);
		System.out.println("service: "+service);
		System.out.println("intentService: "+intentService);
		System.out.println("broadcastReceiver: "+broadcastReceiver);
		System.out.println("contentProvider: "+contentProvider);
		System.out.println("reachableMethod: "+reachableMethod);
		System.out.println("reachableCode: "+reachableCode);
		System.out.println(String.format("asdf: %s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", Main.apkPath, totalMethod, totalCode, activity, service, intentService, broadcastReceiver, contentProvider, reachableMethod, reachableCode));
	}
	
	boolean shouldCount(SootMethod s) {
		String className = s.getDeclaringClass().getName();
		return !(className.startsWith("android")||className.startsWith("com.android."))
				&& s.isConcrete()
				&& s.getDeclaringClass().isApplicationClass();
	}
	
}