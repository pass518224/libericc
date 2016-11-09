package libericc.preevaluate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import libericc.Logger;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class PreEvaluateInstrumenter {
	static String evaloggerPackage = "libericc.preevaluate";
	static String evaloggerName = "PreEvaluateLogger";
	static String evaloggerFullName = evaloggerPackage+"."+evaloggerName;
	static String branchResultString = "<"+evaloggerFullName+": void branchResult(int,int)>";
	static String methodInvokeString = "<"+evaloggerFullName+": void methodInvoke(int)>";
	
	SootMethodRef branchResultRef;
	SootMethodRef methodInvokeRef;
	PrintWriter branchesWriter;
	
	public void instrument(){
		Logger.getLogger().info("Start instrument");
		branchResultRef = Scene.v().getMethod(branchResultString).makeRef();
		methodInvokeRef = Scene.v().getMethod(methodInvokeString).makeRef();
		try {
			branchesWriter = new PrintWriter("branches", "UTF-8");
			for (SootClass sc: new ArrayList<SootClass>(Scene.v().getClasses())) {
				for (SootMethod s: new ArrayList<SootMethod>(sc.getMethods())) {
					if (shouldInstrument(s)) {
						Logger.getLogger().fine("Instrument "+s.toString());
						instrument(s);
					}
				}
			}
			branchesWriter.close();
		} catch (IOException e) {}
		Logger.getLogger().info("End instrument");
	}
	
	boolean shouldInstrument(SootMethod s) {
		String packageName = s.getDeclaringClass().getJavaPackageName();
		return s.getDeclaringClass().isApplicationClass() &&
				s.isConcrete() &&
				!packageName.startsWith("android.") &&
				!packageName.startsWith("com.android.") &&
				!packageName.startsWith("libericc.");
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
	
	boolean isBroadReceiver(String targetClass) {
		try {
			for (SootClass sc = Scene.v().getSootClass(targetClass); sc != null; sc = sc.getSuperclass()) {
				if (sc.toString().equals("android.content.BroadcastReceiver")) {
					return true;
				}
			}
		}
		catch (Exception e) {}
		return false;
	}
	
	void instrument(SootMethod m) {
		PatchingChain<Unit> unitChain = m.retrieveActiveBody().getUnits();
		
		// insert method invocation logging
		Unit first = unitChain.getFirst();
		while (first instanceof IdentityStmt) {
			first = unitChain.getSuccOf(first);
		}
		unitChain.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(methodInvokeRef, IntConstant.v(m.toString().hashCode()))), first);
		
		// collect branches
		List<Stmt> stmtList = new ArrayList<Stmt>();
		for (Unit u: unitChain) {
			if (u instanceof IfStmt || u instanceof SwitchStmt) {
				stmtList.add((Stmt)u);
			}
		}

		// instrument branches
		for (int i=0; i<stmtList.size(); i++) {
			Stmt s = stmtList.get(i);
			branchesWriter.println(String.format("%s:%d %s", m, i, s));
		}
		for (int i=0; i<stmtList.size(); i++) {
			Stmt s = stmtList.get(i);
			if (s instanceof IfStmt) {
				instrumentBranch(m, (IfStmt)s, i);
			}
			else {
				instrumentSwitch(m, (SwitchStmt)s, i);
			}
		}
	}
	
	void instrumentBranch(SootMethod method, IfStmt stmt, int branchId){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value condition = stmt.getCondition();
		Unit logTrueUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchHashCode(method, branchId)), IntConstant.v(1)));
		unitChain.insertBefore(logTrueUnit, stmt);
		Unit gotoEndUnit = Jimple.v().newGotoStmt(stmt);
		unitChain.insertBefore(gotoEndUnit, logTrueUnit);
		Unit logFalseUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchHashCode(method, branchId)), IntConstant.v(0)));
		unitChain.insertBefore(logFalseUnit, gotoEndUnit);
		Unit ifTrueUnit = Jimple.v().newIfStmt(condition, logTrueUnit);
		unitChain.insertBefore(ifTrueUnit, logFalseUnit);
	}
	
	void instrumentSwitch(SootMethod method, SwitchStmt stmt, int branchId){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value key = stmt.getKey();
		Unit logUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchHashCode(method, branchId)), key));
		
		unitChain.insertBefore(logUnit, stmt);
	}
	
	static public int branchHashCode(SootMethod method, int id) {
		return branchHashCode(method.toString(), id);
	}
	
	static public int branchHashCode(String method, int id) {
		return String.format("%s:%d", method, id).hashCode();
	}
	
	static public void storePreEvaluateLogger(){
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Object.class.getResourceAsStream("/"+evaloggerFullName.replace(".", "/")+".class");
			new File("tmp/"+evaloggerPackage.replace(".", "/")).mkdirs();
			out = new FileOutputStream("tmp/"+evaloggerFullName.replace(".", "/")+".class");
			            
			int readBytes;
			byte[] buffer = new byte[4096];
			while ((readBytes = in.read(buffer)) > 0) {
				out.write(buffer, 0, readBytes);
			}
		} catch (IOException e) {}
		finally {
			try {
				in.close();
			} catch (IOException e) {}
			try {
				out.close();
			} catch (IOException e) {}
		}
	}
}
