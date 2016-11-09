package libericc;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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


/*
 * generate dummy main function as analysis entry point
 * the dummy main calls all component life cycle method
 */

public class DummyMainCreator {
	SootClass dummyMainClass;
	SootField iccNo;
	SootMethod mainMethod;
	JimpleBody body;
	PatchingChain<Unit> units;
	List<Integer> usedICC = new ArrayList<Integer>();
	
	public SootMethod createDummyMain(){
		Logger.getLogger().info("Creating dummy main");
		
		createMethod();
		generateBody();
		dumpMethod();
		
		dumpUsedJSON();
		
		return mainMethod;
	}
	
	// create main method
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
	
	// generate dummy main method body
	// call used component life cycle entry method in icc log
	void generateBody() {
		JSONObject iccLogs = Config.getIccLogs();
		List<String> sortedICC = sortKeys(iccLogs);
		Map<String, String> binderToClass = new HashMap<String, String>(); // save the mapping from binder (16-byte hex) to target class
		Map<String, Local> classToLocal = new HashMap<String, Local>(); // allocate a local variable for each used componenet

		// for each icc record
		for (String id: sortedICC){
			JSONObject iccLog = iccLogs.getJSONObject(id);
			boolean processThisICC = Config.iccNo==0||Integer.parseInt(id)==Config.iccNo;
			
			// icc from system_server
			String source = iccLog.getString("Source");

			// procces the icc from system_server to component lifecycle method
			if (source.equals("system_server") || source.equals("system_server]")) {
				Logger.getLogger().finest(String.format("Handle ICC no.%s", id));
				// Insert stmt: "iccNo = id"
				// IDE use iccNo (public static int) to determine the incoming icc
				IntConstant intConst = IntConstant.v(Integer.parseInt(id));
				Unit assignIccNo = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(iccNo.makeRef()), intConst);
				units.addLast(assignIccNo);
				
				String targetClass;
				String binder;
				Local tmpLocal;
				
				// determine whether it is a lifecycle icc from Transact_code and generate codes
				// abstract of each case
				// 1. determine the target class
				//     a. get from icc and add an entry to "binderToClass" map for a creation/starting ICC
				//     b. get from "binderToClass" map for other ICC
				// 2. create an Local or get from "classToLocal" if exist
				// 3. add proper method call if this ICC need to proccess
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
					if (processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onCreate", NullConstant.v());
						addMethodCall(tmpLocal, targetClass, "onStart");
						addMethodCall(tmpLocal, targetClass, "onResume");
					}
					break;
				case "SCHEDULE_SEND_RESULT_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					int requestCode = iccLog.getJSONObject("ri").getInt("mRequestCode");
					int resultCode = iccLog.getJSONObject("ri").getInt("mResultCode");
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onActivityResult", IntConstant.v(requestCode), IntConstant.v(resultCode), NullConstant.v());
					}
					break;
				case "SCHEDULE_RESUME_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onResume");
					}
					break;
				case "SCHEDULE_PAUSE_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onPause");
					}
					break;
				case "SCHEDULE_STOP_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onStop");
					}
					break;
				case "SCHEDULE_FINISH_ACTIVITY_TRANSACTION":
					binder = iccLog.getString("b");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
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
					if (processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onCreate");
					}
					break;
				case "SCHEDULE_SERVICE_ARGS_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null) {
						if (isIntentService(targetClass)) {
							if (processThisICC) {
								usedICC.add(Integer.parseInt(id));
								addMethodCall(tmpLocal, targetClass, "onHandleIntent", NullConstant.v());
							}
						}
						else {
							int startId = Integer.parseInt(iccLog.getString("startId"));
							int flags = Integer.parseInt(iccLog.getString("fl"));
							if (processThisICC) {
								usedICC.add(Integer.parseInt(id));
								addMethodCall(tmpLocal, targetClass, "onStartCommand", NullConstant.v(), IntConstant.v(flags), IntConstant.v(startId));
							}
						}
					}
					break;
				case "SCHEDULE_BIND_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onBind", NullConstant.v());
					}
					break;
				case "SCHEDULE_UNBIND_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onUnbind", NullConstant.v());
					}
					break;
				case "SCHEDULE_STOP_SERVICE_TRANSACTION":
					binder = iccLog.getString("token");
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					if (targetClass != null && processThisICC) {
						usedICC.add(Integer.parseInt(id));
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
					if (processThisICC) {
						usedICC.add(Integer.parseInt(id));
						addMethodCall(tmpLocal, targetClass, "onReceive", NullConstant.v(), NullConstant.v());
					}
					break;
				}
			}
			// codes for processing ICCs to system_server
			/*String target = iccLog.getString("Target");
			try {
				if (target.equals("system_server") || target.equals("system_server]")) {
					String ifaceName = iccLog.getString("Descriptor");
					JSONObject result = iccLog.getJSONObject("Result");
					String methodName = result.getString("Name");
					JSONArray params = result.getJSONArray("Params");
					SootClass iface = Scene.v().getSootClass(ifaceName);
					
					//Logger.getLogger().info(iface+" method count: "+iface.getMethodCount());
					//Logger.getLogger().info("ICC to interface: "+iface);
					if (iface.isInterface()) {
						for (SootClass subClass: Scene.v().getActiveHierarchy().getImplementersOf(iface)) {
							String className = subClass.getName();
							if (className.endsWith("Service")) {
								Local tmpLocal = classToLocal.get(className);
								if (tmpLocal == null) {
									tmpLocal = createLocal(className);
									classToLocal.put(className, tmpLocal);
									initClass(className, tmpLocal);
								}
								SootMethod targetMethod = iface.getMethodByName(methodName);
								Value[] values = new Value[targetMethod.getParameterCount()];
								for (int i=0; i<targetMethod.getParameterCount(); i++) {
									String value = params.getString(i);
									switch (targetMethod.getParameterType(i).toString()){
									case "int":
										values[i] = IntConstant.v(Integer.parseInt(value));
										break;
									case "boolean":
										values[i] = IntConstant.v(value.equals("True")? 1: 0);
										break;
									case "java.lang.String":
										if (value.equals("None")) values[i] = NullConstant.v();
										else values[i] = StringConstant.v(value);
										break;
									case "android.content.Intent":
										values[i] = NullConstant.v();
										break;
									default:
										values[i] = NullConstant.v();
									}
								}
								addMethodCall(tmpLocal, className, methodName, values);
								Handlers.insertHandler(AutoCallHandler.v().new MethodSig(className, targetMethod.getSubSignature()), AutoCallHandler.v());
							}
						}
					}
					else {
						for (SootClass subClass: Scene.v().getActiveHierarchy().getSubclassesOf(iface)) {
							Logger.getLogger().info("\t"+subClass);
						}
					}
				}
			}
			catch (JSONException e) {}*/
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
		Scene.v().getSootClass(targetClass).setResolvingLevel(SootClass.BODIES);
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
			Unit initStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(tmpLocal, Scene.v().getSootClass(targetClass).getMethod("void <init>()").makeRef()));
			units.addLast(initStmt);
		}
		catch (Exception e) {}
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
		Logger.getLogger().finest(String.format("Add method call %s.%s", targetClass, targetMethod));
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
	
	void dumpUsedJSON() {
		JSONObject obj = new JSONObject();
		for (int id: usedICC) {
			String stringid = String.valueOf(id);
			obj.put(stringid, Config.getIccLogs().getJSONObject(stringid));
		}
		Logger.getLogger().finest("Used ICC: "+obj.toString());
		Logger.getLogger().finest("Used ICC Length: "+obj.toString().length());
	}
}
