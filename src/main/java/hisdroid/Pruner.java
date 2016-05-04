package hisdroid;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hisdroid.value.GeneralValue;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.util.queue.QueueReader;

public class Pruner {
	static final Logger logger = Logger.getLogger("IDELog");
	Analyzer analyzer;
	
	public Pruner(Analyzer analyzer){
		this.analyzer = analyzer;
	}
	
	public void prune(){
		for (QueueReader<MethodOrMethodContext> qr = Scene.v().getReachableMethods().listener();qr.hasNext(); ) {
			SootMethod s = qr.next().method();
			if (shouldBePruned(s)) {
				prune(s);
			}
		}
	}
	
	boolean shouldBePruned(SootMethod s) {
		String packageName = s.getDeclaringClass().getJavaPackageName();
		return !s.getDeclaringClass().isLibraryClass()
				&& !s.isJavaLibraryMethod()
				&& !s.isPhantom()
				&& !packageName.startsWith("android.support")
				&& !packageName.startsWith("dalvik.system");
	}
	
	void prune(SootMethod m){
		PatchingChain<Unit> unitChain = m.getActiveBody().getUnits();
		
		logger.info(m.toString());
		for (Unit u: unitChain) {
			Map<Value, GeneralValue> map = analyzer.resultsAt(u);
			for (Value key : map.keySet()) {
				logger.info("\t"+key + ": " + map.get(key));
			}
			logger.info(u.toString());
			if (u instanceof IfStmt) {
				logger.info("\tResult: "+analyzer.branchAt((IfStmt)u));
			}
		}
		
		List<IfStmt> trueList = new LinkedList<IfStmt>();
		List<IfStmt> falseList = new LinkedList<IfStmt>();
		for (Unit u: unitChain) {
			if (u instanceof IfStmt) {
				TriLogic b = analyzer.branchAt((IfStmt)u);
				if (b.isTrue()) {
					trueList.add((IfStmt)u);
				}
				else if (b.isFalse()) {
					falseList.add((IfStmt)u);
				}
			}
		}
		for (IfStmt u: trueList) {
			Unit s = Jimple.v().newGotoStmt(u.getTarget());
			unitChain.insertBefore(s, u);
			unitChain.remove(u);
		}
		for (IfStmt u: falseList) {
			unitChain.remove(u);
		}//*/
	}
	
}
