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
	static final Logger logger = Logger.getLogger("IDELog");

	SootClass dummyMainClass;
	SootField iccNo;
	SootMethod mainMethod;
	JimpleBody body;
	PatchingChain<Unit> units;
	Local tmpIntent;
	
	public SootMethod createDummyMain(){
		logger.info("Creating dummy main");
		
		JSONObject iccLogs = Config.getIccLogs();
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
		// Add locals
		tmpIntent = Jimple.v().newLocal("tmpIntent", RefType.v("android.content.Intent"));
		body.getLocals().add(tmpIntent);

		Scene.v().setEntryPoints(Collections.singletonList(mainMethod));
		units = body.getUnits();
		
		List<String> sortedICC = sortKeys(iccLogs);
		Map<String, String> binderToClass = new HashMap<String, String>();
		Map<String, Local> classToLocal = new HashMap<String, Local>();
		// for each icc record
		for (String id: sortedICC){
			JSONObject iccLog = iccLogs.getJSONObject(id);
			// Insert stmt: "iccNo = id" 
			IntConstant intConst= IntConstant.v(Integer.parseInt(id));
			Unit assignIccNo = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(iccNo.makeRef()), intConst);
			units.addLast(assignIccNo);
			
			// icc from system_server
			if (iccLog.getString("Source").equals("system_server")) {
				String targetClass = null;
				String binder = iccLog.getString("Binder");
				Local tmpLocal;
				
				switch (iccLog.getString("Transact_code")) {
				case "SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION":
					targetClass = iccLog.getString("Class");
					binderToClass.put(binder, targetClass);
					tmpLocal = Jimple.v().newLocal(targetClass.replace(".","_"), RefType.v(targetClass));
					body.getLocals().add(tmpLocal);
					classToLocal.put(targetClass, tmpLocal);
					Expr newExpr = Jimple.v().newNewExpr(Scene.v().getSootClass(targetClass).getType());
					Unit assignStmt = Jimple.v().newAssignStmt(tmpLocal, newExpr);
					units.addLast(assignStmt);
					
					addMethodCall(tmpLocal, targetClass, "onCreate", NullConstant.v());
					addMethodCall(tmpLocal, targetClass, "onStart");
					addMethodCall(tmpLocal, targetClass, "onResume");
					Scene.v().forceResolve(targetClass, SootClass.BODIES);
					Scene.v().loadClassAndSupport(targetClass);
					break;
				case "SCHEDULE_SEND_RESULT_TRANSACTION":
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					int requestCode = iccLog.getInt("requestCode");
					int resultCode = iccLog.getInt("resultCode");
					addAssignStmtWithMethodCall(tmpIntent, tmpLocal, "android.app.Activity", "getIntent");
					addMethodCall(tmpLocal, targetClass, "onActivityResult", IntConstant.v(requestCode), IntConstant.v(resultCode), tmpIntent);
					break;
				case "SCHEDULE_RESUME_ACTIVITY_TRANSACTION":
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					addMethodCall(tmpLocal, targetClass, "onResume");
					break;
				case "SCHEDULE_PAUSE_ACTIVITY_TRANSACTION":
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					addMethodCall(tmpLocal, targetClass, "onPause");
					break;
				case "SCHEDULE_FINISH_ACTIVITY_TRANSACTION":
					targetClass = binderToClass.get(binder);
					tmpLocal = classToLocal.get(targetClass);
					addMethodCall(tmpLocal, targetClass, "onDestory");
				}
			}
		}
		// add return
		Unit returnStmt = Jimple.v().newReturnVoidStmt();
		units.addLast(returnStmt);
		
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
		
		return mainMethod;
	}
	
	void addMethodCall(Local base, String targetClass, String targetMethod, Value ... args){
		logger.finest(String.format("Add method call %s.%s", targetClass, targetMethod));
		try {
			Expr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, Scene.v().getSootClass(targetClass).getMethodByName(targetMethod).makeRef(), args);
			Unit invokeStmt = Jimple.v().newInvokeStmt(invokeExpr);
			units.addLast(invokeStmt);
		}
		catch (Exception e) {}
	}
	
	void addAssignStmtWithMethodCall(Local lvalue, Local base, String targetClass, String targetMethod, Value ... args){
		logger.finest(String.format("Add method call %s.%s", targetClass, targetMethod));
		try {
			Expr invokeExpr = Jimple.v().newVirtualInvokeExpr(base, Scene.v().getSootClass(targetClass).getMethodByName(targetMethod).makeRef(), args);
			Unit assignStmt = Jimple.v().newAssignStmt(lvalue, invokeExpr);
			units.addLast(assignStmt);
		}
		catch (Exception e) {}
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
