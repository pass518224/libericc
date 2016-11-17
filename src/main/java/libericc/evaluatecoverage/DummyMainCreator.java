package libericc.evaluatecoverage;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import libericc.Logger;
import soot.ArrayType;
import soot.DoubleType;
import soot.FloatType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.PatchingChain;
import soot.Printer;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.DoubleConstant;
import soot.jimple.Expr;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.options.Options;

/*
 * Generates lifecycle entries of all components
 * libericc.DummyMainCreator is based on ICC records
 * This is all componenets found in the apk
 */

public class DummyMainCreator {
	SootClass dummyMainClass;
	SootMethod mainMethod;
	JimpleBody body;
	PatchingChain<Unit> units;
	
	public static Set<SootMethod> iccMethods = new HashSet<SootMethod>();
	
	public SootMethod createDummyMain(){
		createMethod();
		generateBody();
		dumpMethod();
		
		return mainMethod;
	}
	
	void createMethod() {
		// Create DummyMain Class
		dummyMainClass = new SootClass("DummyMain", Modifier.PUBLIC);
		dummyMainClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		Scene.v().addClass(dummyMainClass);
		// Create main method
		mainMethod = new SootMethod("main",                 
				Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
				VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
		dummyMainClass.addMethod(mainMethod);
		// Create method body
		body = Jimple.v().newBody(mainMethod);
		mainMethod.setActiveBody(body);

		Scene.v().setEntryPoints(Collections.singletonList(mainMethod));
		Scene.v().setMainClass(dummyMainClass);
		units = body.getUnits();
	}
	
	void generateBody() {
		Map<String, Set<String>> lifeCycleMethods = new HashMap<String, Set<String>>();
		lifeCycleMethods.put("android.app.Activity", new HashSet<String>());
		lifeCycleMethods.put("android.app.Service", new HashSet<String>());
		lifeCycleMethods.put("android.app.IntentService", new HashSet<String>());
		lifeCycleMethods.put("android.content.BroadcastReceiver", new HashSet<String>());
		lifeCycleMethods.put("android.content.ContentProvider", new HashSet<String>());
		
		lifeCycleMethods.get("android.app.Activity").add("onCreate");
		lifeCycleMethods.get("android.app.Activity").add("onStart");
		lifeCycleMethods.get("android.app.Activity").add("onResume");
		lifeCycleMethods.get("android.app.Activity").add("onPause");
		lifeCycleMethods.get("android.app.Activity").add("onStop");
		lifeCycleMethods.get("android.app.Activity").add("onDestory");
		lifeCycleMethods.get("android.app.Activity").add("onActivityResult");
		lifeCycleMethods.get("android.app.Service").add("onCreate");
		lifeCycleMethods.get("android.app.Service").add("onBind");
		lifeCycleMethods.get("android.app.Service").add("onUnBind");
		lifeCycleMethods.get("android.app.Service").add("onStartCommand");
		lifeCycleMethods.get("android.app.Service").add("onDestory");
		lifeCycleMethods.get("android.app.IntentService").add("onHandleIntent");
		lifeCycleMethods.get("android.content.BroadcastReceiver").add("onReceive");
		lifeCycleMethods.get("android.content.ContentProvider").add("onCreate");
		
		for (Map.Entry<String, Set<String>> entry: lifeCycleMethods.entrySet()) {
			String clazz = entry.getKey();
			Set<String> methods = entry.getValue();
			SootClass classclazz =  Scene.v().getSootClass(clazz);
			for (SootClass component: Scene.v().getActiveHierarchy().getSubclassesOf(classclazz)) {
				Local local = createLocal(component.getName());
				for (String m: methods) {
					try {
						addMethodCall(local, component, component.getMethodByName(m));
						iccMethods.add(component.getMethodByName(m));
					}
					catch (Exception e) {}
				}
			}
		}
		
		// add return
		Unit returnStmt = Jimple.v().newReturnVoidStmt();
		units.addLast(returnStmt);
		
		Logger.getLogger().fine("Total Icc Methods: "+iccMethods.size());
		for (SootMethod s: iccMethods) {
			Logger.getLogger().fine(s.toString());
		}
	}
	
	Map<String, Local> name2local = new HashMap<String, Local>();
	Local createLocal(String targetClass){
		if (!name2local.containsKey(targetClass)) {
			Local tmpLocal = Jimple.v().newLocal(targetClass.replace(".","_").replace("$", "_"), RefType.v(targetClass));
			body.getLocals().add(tmpLocal);
			name2local.put(targetClass, tmpLocal);
			initClass(targetClass, tmpLocal);
			return tmpLocal;
		}
		else {
			return name2local.get(targetClass);
		}
	}
	
	void initClass(String targetClass, Local tmpLocal){
		try {
			Expr newExpr = Jimple.v().newNewExpr(Scene.v().getSootClass(targetClass).getType());
			Unit assignStmt = Jimple.v().newAssignStmt(tmpLocal, newExpr);
			units.addLast(assignStmt);
		}
		catch (Exception e) {}
		try {
			Unit clinitStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getSootClass(targetClass).getMethod("void <clinit>()").makeRef()));
			units.addLast(clinitStmt);
		}
		catch (Exception e) {}
		try {
			SootClass clazz = Scene.v().getSootClass(targetClass);
			for (SootMethod m: clazz.getMethods()) {
				if (m.getName().equals("<init>")) {
					Value args[] = new Value[m.getParameterCount()];
					for (int i=0; i<m.getParameterCount(); i++) {
						Type t = m.getParameterType(i);
						if (t instanceof RefLikeType) args[i]=NullConstant.v();
						else if (t instanceof DoubleType) args[i]=DoubleConstant.v(0);
						else if (t instanceof FloatType) args[i]=FloatConstant.v(0);
						else if (t instanceof LongType) args[i]=LongConstant.v(0);
						else args[i]=IntConstant.v(0);
					}
					Unit initStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(tmpLocal, m.makeRef(), args));
					units.addLast(initStmt);
					break;
				}
			}
		}
		catch (Exception e) {
			Logger.getLogger().fine(e.getMessage());
		}
	}
	
	void addMethodCall(Local base, SootClass targetClass, SootMethod targetMethod){
		try {
			targetClass.setResolvingLevel(SootClass.BODIES);
			Value args[] = new Value[targetMethod.getParameterCount()];
			for (int i=0; i<targetMethod.getParameterCount(); i++) {
				Type t = targetMethod.getParameterType(i);
				if (t instanceof RefLikeType) args[i]=NullConstant.v();
				else if (t instanceof DoubleType) args[i]=DoubleConstant.v(0);
				else if (t instanceof FloatType) args[i]=FloatConstant.v(0);
				else if (t instanceof LongType) args[i]=LongConstant.v(0);
				else args[i]=IntConstant.v(0);
			}
			Expr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, targetMethod.makeRef(), args);
			Unit invokeStmt = Jimple.v().newInvokeStmt(invokeExpr);
			units.addLast(invokeStmt);
		}
		catch (Exception e) {
			Logger.getLogger().fine(e.getMessage());
		}
	}

	void dumpMethod(){
		// write dummyMain to file
		SootClass dummyMainClass = mainMethod.getDeclaringClass();
		try {
			String fileName = SourceLocator.v().getFileNameFor(dummyMainClass, Options.output_format_jimple);
			OutputStream streamOut = new FileOutputStream(fileName);
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			Printer.v().printTo(dummyMainClass, writerOut);
			writerOut.flush();
			streamOut.close();
		} catch (Exception e) {}
	}
}
