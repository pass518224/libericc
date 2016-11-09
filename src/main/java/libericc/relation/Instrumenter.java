package libericc.relation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.JastAddJ.ReturnStmt;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;

public class Instrumenter {
	int totalClass = 0;
	int totalMethod = 0;
	int totalBranch = 0;
	int iccEntryMethod = 0;
	int iccReachableMethod = 0;
	int iccReachableBranch = 0;
	int iccReachableCodeBlock = 0;
	int iccReachableCodeBlockSize = 0;
	int iccDecidableBranch = 0;
	int iccDecidableCodeBlock = 0;
	int iccDecidableCodeBlockSize = 0;
	
	IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;
	SootClass instCounter = Scene.v().getSootClass("inst.InstCounter");
	SootMethodRef branchResultRef = instCounter.getMethodByName("branchResult").makeRef();
	SootMethodRef iccEntryInvokeRef = instCounter.getMethodByName("iccEntryInvoke").makeRef();
	SootMethodRef normalMethodInvokeRef = instCounter.getMethodByName("normalMethodInvoke").makeRef();
	PrintWriter branches;
	PrintWriter entries;
	PrintWriter methods;
	
	public Instrumenter(IFDSSolver<Unit, Value, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver){
		this.solver = solver;
	}
	
	public void instrument(){
		for (SootClass sc: Scene.v().getClasses()) {
			try {
				if (sc.isApplicationClass()) {
					totalClass++;
				}
				for (SootMethod sm: sc.getMethods()) {
					try {
						boolean isIccEntry = IFDSDummyMainCreator.iccMethods.contains(sm);
						for (Unit u: sm.retrieveActiveBody().getUnits()) {
							if (u instanceof IfStmt || u instanceof SwitchStmt) {
								totalBranch++;
							}
						}
						totalMethod++;
						if (isIccEntry) iccEntryMethod++;
					}
					catch (Exception e) {}
				}
			} catch (Exception e) {}
		}
		try {
			branches = new PrintWriter("branches", "UTF-8");
			entries = new PrintWriter("entries", "UTF-8");
			methods = new PrintWriter("methods", "UTF-8");
			List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
			for (SootClass sc: lsc) {
				List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
				for (SootMethod s: lsm) {
					if (shouldInstrument(s)) {
						instrument(s);
					}
				}
			}
			branches.close();
			entries.close();
			methods.close();
		} catch (IOException e) {}
		System.out.println("Total class: "+totalClass);
		System.out.println("Total method: "+totalMethod);
		System.out.println("Total branch: "+totalBranch);
		System.out.println("ICC Entry Method: "+iccEntryMethod);
		System.out.println("ICC Reachable Method: "+iccReachableMethod);
		System.out.println("ICC Reachable Branch: "+iccReachableBranch);
		System.out.println("ICC Reachable Code Block: "+iccReachableCodeBlock);
		System.out.println("ICC Reachable Code Block Size: "+iccReachableCodeBlockSize);
		System.out.println("ICC Decidable Branch: "+iccDecidableBranch);
		System.out.println("ICC Decidable Code Block: "+iccDecidableCodeBlock);
		System.out.println("ICC Decidable Code Block Size: "+iccDecidableCodeBlockSize);
	}
	
	boolean shouldInstrument(SootMethod s) {
		String className = s.getDeclaringClass().getName();
		return !(className.startsWith("android")||className.startsWith("com.android."))
				&& s.isConcrete()
				&& s.getDeclaringClass().isApplicationClass();
	}
	
	boolean isIccBranch(Unit u){
		if (u instanceof IfStmt) {
			IfStmt ifstmt = (IfStmt) u;;
			Value c = ifstmt.getCondition();
			if (c instanceof ConditionExpr) {
				ConditionExpr ce = (ConditionExpr) c;
				Value op1 = ce.getOp1();
				Value op2 = ce.getOp2();
				return ((op1 instanceof Constant)||solver.ifdsResultsAt(u).contains(op1))&&((op2 instanceof Constant)|| solver.ifdsResultsAt(u).contains(op2));
			}
		}
		else if (u instanceof SwitchStmt) {
			SwitchStmt switchstmt = (SwitchStmt) u;;
			Value key = switchstmt.getKey();
			return solver.ifdsResultsAt(u).contains(key);
		}
		return false;
	}
	
	void instrument(SootMethod m) {
		try {
			PatchingChain<Unit> unitChain = m.retrieveActiveBody().getUnits();
			iccReachableMethod++;
			
			System.out.println(m+" "+IFDSDummyMainCreator.iccMethods.contains(m));
			// Find branches
			List<Stmt> stmtList = new ArrayList<Stmt>();
			for (Unit u: unitChain) {
				System.out.println("\t"+u);
				if (u instanceof IfStmt || u instanceof SwitchStmt) {
					stmtList.add((Stmt)u);
					iccReachableBranch++;
					if (isIccBranch(u)) iccDecidableBranch++;
					System.out.println("\t\t"+isIccBranch(u));
				}
			}
			// instrument at method invoke
			// output methods
			PatchingChain<Unit> units = m.retrieveActiveBody().getUnits();
			Unit u = units.getFirst();
			while (u instanceof IdentityStmt) u = units.getSuccOf(u);
			if (IFDSDummyMainCreator.iccMethods.contains(m)) {
				Unit iccInvokeLog = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(iccEntryInvokeRef, IntConstant.v(m.toString().hashCode())));
				units.insertBeforeNoRedirect(iccInvokeLog, u);
				int cbs = codeBlockSize(units, u);
				entries.println(String.format("%s|%d|%d", m.toString(), m.toString().hashCode(), cbs));
				addCodeBlockSize(cbs, true);
			}
			else {
				Unit methodInvokeLog = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(normalMethodInvokeRef, IntConstant.v(m.toString().hashCode())));
				units.insertBeforeNoRedirect(methodInvokeLog, u);
				int cbs = codeBlockSize(units, u);
				methods.println(String.format("%s|%d|%d", m.toString(), m.toString().hashCode(), cbs));
				addCodeBlockSize(cbs, false);
			}
	
			// output branches
			for (int i=0; i<stmtList.size(); i++) {
				Stmt s = stmtList.get(i);
				boolean isIcc = isIccBranch(s);
				if (s instanceof IfStmt) {
					IfStmt ss = (IfStmt) s;
					int cbs0 = codeBlockSize(units, units.getSuccOf(s));
					int cbs1 = codeBlockSize(units, ss.getTarget());
					String results = String.format("{0=>%d, 1=>%d}", cbs0, cbs1);
					branches.println(String.format("%s|%s|%d|%d|%s", isIcc? "[ICCBranch]": "[NotICCBranch]", m, i, branchHashCode(m, i), results));
					// [isIccBranch]:MethodSig:Id:Hashcode:Result
					addCodeBlockSize(cbs0, isIcc);
					addCodeBlockSize(cbs1, isIcc);
				}
				else {
					String results = "{";
					if (s instanceof TableSwitchStmt) {
						TableSwitchStmt ss = (TableSwitchStmt) s;
						int cbs = codeBlockSize(units, ss.getDefaultTarget());
						results+=String.format("'default'=>%d", cbs);
						addCodeBlockSize(cbs, isIcc);
						for (int j=ss.getLowIndex(); j<=ss.getHighIndex(); j++) {
							cbs = codeBlockSize(units, ss.getTarget(j-ss.getLowIndex()));
							results+=String.format(", %d=>%d", j, cbs);
							addCodeBlockSize(cbs, isIcc);
						}
					}
					else {
						LookupSwitchStmt ss = (LookupSwitchStmt) s;
						int cbs = codeBlockSize(units, ss.getDefaultTarget());
						addCodeBlockSize(cbs, isIcc);
						results+=String.format("'default'=>%d", cbs);
						for (int j=0; j<ss.getTargetCount(); j++) {
							cbs = codeBlockSize(units, ss.getTarget(j));
							results+=String.format(", %d=>%d", ss.getLookupValue(j), cbs);
							addCodeBlockSize(cbs, isIcc);
						}
					}
					results+="}";
					branches.println(String.format("%s|%s|%d|%d|%s", isIccBranch(s)? "[ICCBranch]": "[NotICCBranch]", m, i, branchHashCode(m, i), results));
					// [isIccBranch]:MethodSig:Id:Hashcode:Result
				}
			}
			// instrument branches
			for (int i=0; i<stmtList.size(); i++) {
				Stmt s = stmtList.get(i);
				if (s instanceof IfStmt) {
					instrumentBranch(m, (IfStmt)s, branchHashCode(m, i));
				}
				else {
					instrumentSwitch(m, (SwitchStmt)s, branchHashCode(m, i));
				}
			}
		}
		catch (Exception e) {}
	}
	
	int codeBlockSize(PatchingChain<Unit> units, Unit u) {
		int i=0;
		Set<Unit> history = new HashSet<Unit>();
		while (u!=null) {
			i++;
			if (history.contains(u)
					|| u instanceof IfStmt
					|| u instanceof SwitchStmt
					|| u instanceof RetStmt
					|| u instanceof ReturnStmt
					|| u instanceof ReturnVoidStmt
					|| u instanceof ThrowStmt)
			{
				break;
			}
			history.add(u);
			if (u instanceof GotoStmt) {
				u = ((GotoStmt)u).getTarget();
			}
			else {
				u = units.getSuccOf(u);
			}
		}
		return i;
	}
	
	void addCodeBlockSize(int size, boolean isIcc) {
		if (isIcc) {
			iccDecidableCodeBlock++;
			iccDecidableCodeBlockSize+=size;
		}
		iccReachableCodeBlock++;
		iccReachableCodeBlockSize+=size;
	}
	
	int branchHashCode(SootMethod m, int i) {
		return String.format("%s:%d", m, i).hashCode();
	}
	
	void instrumentBranch(SootMethod method, IfStmt stmt, int branchId){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value condition = stmt.getCondition();
		Unit branchTrueUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchId), IntConstant.v(1)));
		unitChain.insertBefore(branchTrueUnit, stmt);
		Unit gotoEndUnit = Jimple.v().newGotoStmt(stmt);
		unitChain.insertBefore(gotoEndUnit, branchTrueUnit);
		Unit branchFalseUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchId), IntConstant.v(0)));
		unitChain.insertBefore(branchFalseUnit, gotoEndUnit);
		Unit ifTrueUnit = Jimple.v().newIfStmt(condition, branchTrueUnit);
		unitChain.insertBefore(ifTrueUnit, branchFalseUnit);
	}
	
	void instrumentSwitch(SootMethod method, SwitchStmt stmt, int branchId){
		PatchingChain<Unit> unitChain = method.getActiveBody().getUnits();
		Value key = stmt.getKey();
		Unit logUnit = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(branchResultRef, IntConstant.v(branchId), key));
		unitChain.insertBefore(logUnit, stmt);
	}
	
	
	static public void storeInstCounter(){
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Object.class.getResourceAsStream("/inst/InstCounter.class");
			new File("tmp/inst").mkdirs();
			out = new FileOutputStream("tmp/inst/InstCounter.class");
			            
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
