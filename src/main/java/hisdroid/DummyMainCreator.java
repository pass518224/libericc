package hisdroid;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;

import soot.ArrayType;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.Expr;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.options.Options;

public class DummyMainCreator {
	static final Logger logger = Logger.getLogger("HisDroid");

	SootClass dummyMainClass;
	SootField iccNo;
	SootMethod mainMethod;
	JimpleBody body;
	PatchingChain<Unit> units;
	
	public SootMethod createDummyMain(){
		logger.info("Creating dummy main");
		
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
		// Create public static field int iccNo
		iccNo = new SootField("iccNo", IntType.v(), Modifier.PUBLIC | Modifier.STATIC);
		dummyMainClass.addField(iccNo);
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
		JSONObject iccLogs = Config.getIccLogs();
		List<String> sortedICC = sortKeys(iccLogs);
		Map<String, String> binderToClass = new HashMap<String, String>();
		Map<String, Local> classToLocal = new HashMap<String, Local>();
		// for each icc record
		for (String id: sortedICC){
			JSONObject iccLog = iccLogs.getJSONObject(id);
			
			// icc from system_server
			String source = iccLog.getString("Source");
			if (source.equals("system_server") || source.equals("system_server]")) {
				logger.finest(String.format("Handle ICC no.%s", id));
				// Insert stmt: "iccNo = id" 
				IntConstant intConst= IntConstant.v(Integer.parseInt(id));
				Unit assignIccNo = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(iccNo.makeRef()), intConst);
				units.addLast(assignIccNo);
				
				String targetClass;
				String binder;
				Local tmpLocal;
				
				switch (iccLog.getString("Transact_code")) {
				case "SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = iccLog.getJSONObject("intent").getJSONObject("mComponent").getString("mClass");
					binderToClass.put(binder, targetClass);
					tmpLocal = classToLocal.get(targetClass);
					if (tmpLocal == null) {
						tmpLocal = createLocal(targetClass);
						classToLocal.put(targetClass, tmpLocal);
						initClass(targetClass, tmpLocal);
					}
					addMethodCall(tmpLocal, targetClass, "onCreate", NullConstant.v());
					addMethodCall(tmpLocal, targetClass, "onStart");
					addMethodCall(tmpLocal, targetClass, "onResume");
					break;
				case "SCHEDULE_SEND_RESULT_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					int requestCode = iccLog.getJSONObject("ri").getInt("mRequestCode");
					int resultCode = iccLog.getJSONObject("ri").getInt("mResultCode");
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onActivityResult", IntConstant.v(requestCode), IntConstant.v(resultCode), NullConstant.v());
					}
					break;
				case "SCHEDULE_RESUME_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onResume");
					}
					break;
				case "SCHEDULE_PAUSE_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onPause");
					}
					break;
				case "SCHEDULE_STOP_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onStop");
					}
					break;
				case "SCHEDULE_FINISH_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onDestroy");
					}
					break;
				case "SCHEDULE_CREATE_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = iccLog.getJSONObject("info").getString("name");
					binderToClass.put(binder, targetClass);
					tmpLocal = classToLocal.get(targetClass);
					if (tmpLocal == null) {
						tmpLocal = createLocal(targetClass);
						classToLocal.put(targetClass, tmpLocal);
						initClass(targetClass, tmpLocal);
					}
					addMethodCall(tmpLocal, targetClass, "onCreate");
					break;
				case "SCHEDULE_SERVICE_ARGS_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						if (isIntentService(targetClass)) {
							addMethodCall(tmpLocal, targetClass, "onHandleIntent", NullConstant.v());
						}
						else {
							int startId = Integer.parseInt(iccLog.getString("startId"));
							int flags = Integer.parseInt(iccLog.getString("fl"));
							addMethodCall(tmpLocal, targetClass, "onStartCommand", NullConstant.v(), IntConstant.v(flags), IntConstant.v(startId));
						}
					}
					break;
				case "SCHEDULE_BIND_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onBind", NullConstant.v());
					}
					break;
				case "SCHEDULE_UNBIND_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onUnbind", NullConstant.v());
					}
					break;
				case "SCHEDULE_STOP_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						addMethodCall(tmpLocal, targetClass, "onDestroy");
					}
					break;
				case "SCHEDULE_RECEIVER_TRANSACTION":
					targetClass = iccLog.getJSONObject("intent").getJSONObject("mComponent").getString("mClass");
					tmpLocal = classToLocal.get(targetClass);
					if (tmpLocal == null) {
						tmpLocal = createLocal(targetClass);
						classToLocal.put(targetClass, tmpLocal);
						initClass(targetClass, tmpLocal);
					}
					addMethodCall(tmpLocal, targetClass, "onReceive", NullConstant.v(), NullConstant.v());
					break;
				}
			}
		}
		// add return
		Unit returnStmt = Jimple.v().newReturnVoidStmt();
		units.addLast(returnStmt);
	}
	
	Local createLocal(String targetClass){
		Local tmpLocal = Jimple.v().newLocal(targetClass.replace(".","_"), RefType.v(targetClass));
		body.getLocals().add(tmpLocal);
		return tmpLocal;
	}
	
	void initClass(String targetClass, Local tmpLocal){
		Expr newExpr = Jimple.v().newNewExpr(Scene.v().getSootClass(targetClass).getType());
		Unit assignStmt = Jimple.v().newAssignStmt(tmpLocal, newExpr);
		Unit clinitStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getSootClass(targetClass).getMethod("void <clinit>()").makeRef()));
		Unit initStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(tmpLocal, Scene.v().getSootClass(targetClass).getMethod("void <init>()").makeRef()));
		units.addLast(assignStmt);
		units.addLast(clinitStmt);
		units.addLast(initStmt);
	}
	
	boolean isIntentService(String targetClass) {
		try {
			for (SootClass sc = Scene.v().getSootClass(targetClass); sc != null; sc = sc.getSuperclass()) {
				if (sc.toString().equals("android.app.IntentService")) {
					return true;
				}
			}
		}
		catch (Exception e) {}
		return false;
	}
	
	void addMethodCall(Local base, String targetClass, String targetMethod, Value ... args){
		logger.finest(String.format("Add method call %s.%s", targetClass, targetMethod));
		try {
			for (SootClass sc = Scene.v().getSootClass(targetClass); sc != null; sc = sc.getSuperclass()) {
				sc.setResolvingLevel(SootClass.BODIES);
				if (sc.declaresMethodByName(targetMethod)) {
					Expr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, sc.getMethodByName(targetMethod).makeRef(), args);
					Unit invokeStmt = Jimple.v().newInvokeStmt(invokeExpr);
					units.addLast(invokeStmt);
					break;
				}
			}
		}
		catch (Exception e) {}
	}
	
	/*void addAssignStmtWithMethodCall(Local lvalue, Local base, String targetClass, String targetMethod, Value ... args){
		logger.finest(String.format("Add method call %s.%s", targetClass, targetMethod));
		try {
			Expr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, Scene.v().getSootClass(targetClass).getMethodByName(targetMethod).makeRef(), args);
			Unit assignStmt = Jimple.v().newAssignStmt(lvalue, invokeExpr);
			units.addLast(assignStmt);
		}
		catch (Exception e) {}
	}*/
	
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
	
	static List<String> sortKeys(JSONObject iccLogs){
		List<String> ret = new LinkedList<String>(iccLogs.keySet());
		Collections.sort(ret, new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return Integer.parseInt(o1) - Integer.parseInt(o2);
		    }
		});
		return ret;
	}
}
