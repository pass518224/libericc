package libericc.preevaluate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.SwitchStmt;

/*
 * Experimental Logger
 * Log every parameter of method invocation
 */


public class PreEvaluateInstrumenterTest {
	static protected final Logger logger = Logger.getLogger("Libericc");
	
	static String evaloggerPackage = "libericc.preevaluate";
	static String evaloggerName = "PreEvaluateLoggerTest";
	static String evaloggerFullName = evaloggerPackage+"."+evaloggerName;
	static String intentToStringMethod = "<"+evaloggerFullName+": java.lang.String intentToString(android.content.Intent)>";
	static String bundleToStringMethod = "<"+evaloggerFullName+": java.lang.String bundleToString(android.os.Bundle)>";
	static String methodInvokeMethod = "<"+evaloggerFullName+": void methodInvoke(java.lang.String,java.lang.String,int)>";
	static String branchResultMethod = "<"+evaloggerFullName+": void branchResult(java.lang.String,int,int,int)>";
	
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
				!packageName.startsWith("libericc.") &&
				!s.getName().equals("toString");
	}

	void instrument(SootMethod m) {
		Local hash = instrumentMethod(m);
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
				instrumentBranch(m, (IfStmt)s, i, hash);
			}
			else {
				instrumentSwitch(m, (SwitchStmt)s, i, hash);
			}
		}
	}
	
	void instrumentBranch(SootMethod method, IfStmt stmt, int branchId, Local hash){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value condition = stmt.getCondition();
		Unit logTrueUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(branchResultMethod).makeRef(), StringConstant.v(method.toString()), IntConstant.v(branchId), IntConstant.v(1), hash));
		unitChain.insertBefore(logTrueUnit, stmt);
		Unit gotoEndUnit = Jimple.v().newGotoStmt(stmt);
		unitChain.insertBefore(gotoEndUnit, logTrueUnit);
		Unit logFalseUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(branchResultMethod).makeRef(), StringConstant.v(method.toString()), IntConstant.v(branchId), IntConstant.v(0), hash));
		unitChain.insertBefore(logFalseUnit, gotoEndUnit);
		Unit ifTrueUnit = Jimple.v().newIfStmt(condition, logTrueUnit);
		unitChain.insertBefore(ifTrueUnit, logFalseUnit);
	}
	
	void instrumentSwitch(SootMethod method, SwitchStmt stmt, int branchId, Local hash){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value key = stmt.getKey();
		Unit logUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(branchResultMethod).makeRef(), StringConstant.v(method.toString()), IntConstant.v(branchId), key, hash));
		
		unitChain.insertBefore(logUnit, stmt);
	}
	
	Local instrumentMethod(SootMethod method) {
		// usually used methods
		SootMethodRef stringBuilderAppendString = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.StringBuilder append(java.lang.String)").makeRef();
		
		PatchingChain<Unit> unitChain = method.retrieveActiveBody().getUnits();
		Unit u, firstStmt = unitChain.getFirst();
		while (firstStmt instanceof IdentityStmt) firstStmt = unitChain.getSuccOf(firstStmt);
		
		// add locals
		Local argstring = Jimple.v().newLocal("instrument_argstring", RefType.v("java.lang.StringBuilder"));
		method.retrieveActiveBody().getLocals().add(argstring);
		Local tmpstring = Jimple.v().newLocal("instrument_tmpstring", RefType.v("java.lang.String"));
		method.retrieveActiveBody().getLocals().add(tmpstring);
		Local hash = Jimple.v().newLocal("instrument_hash", IntType.v());
		method.retrieveActiveBody().getLocals().add(hash);
		
		// argstring = new StringBuilder();
		u = Jimple.v().newAssignStmt(argstring, Jimple.v().newNewExpr(RefType.v("java.lang.StringBuilder")));
		unitChain.insertBeforeNoRedirect(u, firstStmt);
		u = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(argstring, Scene.v().getSootClass("java.lang.StringBuilder").getMethod("void <init>()").makeRef()));
		unitChain.insertBeforeNoRedirect(u, firstStmt);
		
		for (int i=0; i<method.getParameterCount(); i++) {
			Type paramType = method.getParameterType(i);
			Local param = method.retrieveActiveBody().getParameterLocal(i);
			
			if (paramType instanceof RefType) {
				if (((RefType)paramType).getSootClass().getName().equals("android.content.Intent")) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(intentToStringMethod).makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				if (((RefType)paramType).getSootClass().getName().equals("android.os.Bundle")) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(bundleToStringMethod).makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else {
					// tmpstring = String.valueOf(param)
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(java.lang.Object)>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
			}
			else if (paramType instanceof IntType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(int)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof LongType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(long)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof DoubleType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(double)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof FloatType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(float)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof BooleanType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(boolean)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof CharType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(char)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof ShortType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.Short: java.lang.String toString(short)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof ByteType) {
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.Byte: java.lang.String toString(byte)>").makeRef(), param));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			else if (paramType instanceof ArrayType) {
				Type elementType = ((ArrayType)paramType).getElementType();
				if (elementType instanceof RefType) {
					// tmpstring = String.valueOf(element)
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(java.lang.Object[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof IntType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(int[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof LongType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(long[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof DoubleType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(double[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof FloatType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(float[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof BooleanType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(boolean[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof CharType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(char[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof ByteType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(byte[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else if (elementType instanceof ShortType) {
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.util.Arrays: java.lang.String toString(short[])>").makeRef(), param));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
				else {
					logger.warning("Unhandled type "+paramType+" at "+method);
					// tmpstring = new String();
					u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newNewExpr(RefType.v("java.lang.String")));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
					u = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(tmpstring, Scene.v().getSootClass("java.lang.String").getMethod("void <init>()").makeRef()));
					unitChain.insertBeforeNoRedirect(u, firstStmt);
				}
			}
			else {
				logger.warning("Unhandled type "+paramType+" at "+method);
				// tmpstring = new String();
				u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newNewExpr(RefType.v("java.lang.String")));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
				u = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(tmpstring, Scene.v().getSootClass("java.lang.String").getMethod("void <init>()").makeRef()));
				unitChain.insertBeforeNoRedirect(u, firstStmt);
			}
			// argstring.append(tmpstring)
			u = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(argstring, stringBuilderAppendString, tmpstring));
			unitChain.insertBeforeNoRedirect(u, firstStmt);

			// argstring.append(", ");
			u = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(argstring, stringBuilderAppendString, StringConstant.v("[Thisisd3mi1eter]")));
			unitChain.insertBeforeNoRedirect(u, firstStmt);
		}
		
		// hash = argstring.toString().hashCode();
		u = Jimple.v().newAssignStmt(tmpstring, Jimple.v().newVirtualInvokeExpr(argstring, Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.String toString()").makeRef()));
		unitChain.insertBeforeNoRedirect(u, firstStmt);
		u = Jimple.v().newAssignStmt(hash, Jimple.v().newVirtualInvokeExpr(tmpstring, Scene.v().getSootClass("java.lang.String").getMethod("int hashCode()").makeRef()));
		unitChain.insertBeforeNoRedirect(u, firstStmt);
		u = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(methodInvokeMethod).makeRef(), StringConstant.v(method.toString()), tmpstring, hash));
		unitChain.insertBeforeNoRedirect(u, firstStmt);
		
		return hash;
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
