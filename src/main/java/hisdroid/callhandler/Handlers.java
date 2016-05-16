package hisdroid.callhandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import hisdroid.callhandler.CallHandler.MethodSig;
import hisdroid.callhandler.intent.*;
import hisdroid.callhandler.bundle.*;
import hisdroid.callhandler.lifecycle.*;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class Handlers {
	// methodSubSignature -> (superClass -> Handler)
	static Map<String, Map<SootClass, CallHandler>> m = new HashMap<String, Map<SootClass, CallHandler>>();
	static CallHandler ignoredCallHandler = new IgnoredCallHandler();
	
	static {
		insertHandler(new StringInitHandler());
		insertHandler(new StringEqualsHandler());
		insertHandler(new GetIntentHandler());

		// Life Cycle
		insertHandler(new ActivityOnActivityResultHandler());
		insertHandler(new ServiceOnBindHandler());
		insertHandler(new ServiceOnUnbindHandler());
		insertHandler(new ServiceOnStartCommandHandler());
		insertHandler(new IntentServiceOnHandleIntentHandler());
		insertHandler(new BroadcastReceiverOnReceiveHandler());
		// Intent
		insertHandler(new IntentGetActionHandler());
		insertHandler(new IntentGetBooleanHandler());
		insertHandler(new IntentGetBundleHandler());
		insertHandler(new IntentGetByteHandler());
		insertHandler(new IntentGetDoubleHandler());
		insertHandler(new IntentGetExtrasHandler());
		insertHandler(new IntentGetFloatHandler());
		insertHandler(new IntentGetIntHandler());
		insertHandler(new IntentGetLongHandler());
		insertHandler(new IntentGetShortHandler());
		insertHandler(new IntentGetStringHandler());
		insertHandler(new IntentHasExtraHandler());
		// Bundle
		insertHandler(new BundleGetBooleanHandler());
		insertHandler(new BundleGetBundleHandler());
		insertHandler(new BundleGetByteHandler());
		insertHandler(new BundleGetDoubleHandler());
		insertHandler(new BundleGetFloatHandler());
		insertHandler(new BundleGetIntHandler());
		insertHandler(new BundleGetLongHandler());
		insertHandler(new BundleGetShortHandler());
		insertHandler(new BundleGetStringHandler());
	}
	
	static public CallHandler getHandler(SootMethod method){
		Map<SootClass, CallHandler> m2 = m.get(method.getSubSignature());
		if (m2 != null) {
			try {
				for (SootClass c = method.getDeclaringClass(); c != null; c = c.getSuperclass()) {
					CallHandler ch = m2.get(c);
					if (ch != null) {
						return ch;
					}
				}
			}
			catch (Exception e) {}
		}
		if (isIgnoredMethod(method)) return ignoredCallHandler;
		return null;
	}
	
	static public void insertHandler(CallHandler handler){
		Set<MethodSig> sigs = handler.getTargets();
		for (MethodSig sig: sigs){
			String methodSubSignature = sig.subSignature;
			SootClass declaringClass = Scene.v().getSootClass(sig.className);
			Map<SootClass, CallHandler> m2 = m.get(methodSubSignature);
			if (m2 == null) {
				m2 = new HashMap<SootClass, CallHandler>();
				m.put(methodSubSignature, m2);
			}
			m2.put(declaringClass, handler);
		}
	}
	
	static public boolean isIgnoredMethod(SootMethod m) {
		String packageName = m.getDeclaringClass().getJavaPackageName();
		return m.isJavaLibraryMethod()
				|| m.isPhantom()
				|| packageName.startsWith("android")
				|| packageName.startsWith("com.google")
				|| packageName.startsWith("com.android")
				|| packageName.startsWith("dalvik.system");
	}
}
