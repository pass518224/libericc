package hisdroid.instrumenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class PreEvaluateInstrumenter {
	static protected final Logger logger = Logger.getLogger("HisDroid");
	SootMethodRef branchResultRef = Scene.v().getSootClass("hisdroid.instrumenter.PreEvaluateLogger").getMethodByName("branchResult").makeRef();
	PrintWriter writer;
	
	public void instrument(){
		logger.info("Start instrument");
		try {
			writer = new PrintWriter("branches", "UTF-8");
			List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
			for (SootClass sc: lsc) {
				List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
				for (SootMethod s: lsm) {
					if (shouldInstrument(s)) {
						logger.fine("Instrument "+s.toString());
						instrument(s);
					}
				}
			}
			writer.close();
		} catch (IOException e) {}
		logger.info("End instrument");
	}
	
	boolean shouldInstrument(SootMethod s) {
		String packageName = s.getDeclaringClass().getJavaPackageName();
		return s.getDeclaringClass().isApplicationClass() &&
				s.isConcrete() &&
				!packageName.startsWith("android.") &&
				!packageName.startsWith("com.android.") &&
				!packageName.startsWith("hisdroid.") && 
				(isIntentService(s.getDeclaringClass().getName()) || isBroadReceiver(s.getDeclaringClass().getName()));
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
		
		List<Stmt> stmtList = new ArrayList<Stmt>();
		
		for (Unit u: unitChain) {
			if (u instanceof IfStmt || u instanceof SwitchStmt) {
				stmtList.add((Stmt)u);
			}
		}

		for (int i=0; i<stmtList.size(); i++) {
			Stmt s = stmtList.get(i);
			writer.println(String.format("%s:%d %s", m, i, s));
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
	
	static public int branchHashCode(SootMethod m, int i) {
		return String.format("%s:%d", m, i).hashCode();
	}
	
	static public void storePreEvaluateLogger(){
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Object.class.getResourceAsStream("/hisdroid/instrumenter/PreEvaluateLogger.class");
			new File("tmp/hisdroid/instrumenter").mkdirs();
			out = new FileOutputStream("tmp/hisdroid/instrumenter/PreEvaluateLogger.class");
			            
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
