package libericc.relation.decider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import libericc.Logger;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.queue.QueueReader;

public class ICCRelationDepthDecider implements Decider {
	int depth;
	ICCRelationDecider relationDecider;
	JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();

	Map<Stmt, Integer> stmt2Depth = new HashMap<Stmt, Integer>();
	
	public ICCRelationDepthDecider(ICCRelationDecider relationDecider, int depth){
		this.relationDecider = relationDecider;
		this.depth = depth;
		BFS();
	}
	
	class StmtDepth {
		public Stmt stmt;
		public int depth;
		public StmtDepth(Stmt stmt, int depth) {
			this.stmt = stmt;
			this.depth = depth;
		}
	}
	
	void BFS() {
		Logger.getLogger().info("Start BFS algorithm");
		Queue<StmtDepth> queue = new ArrayDeque<StmtDepth>();
		Logger.getLogger().info("Init BFS");
		initBFS(queue);

		Logger.getLogger().info("Traverse BFS");
		// traverse
		while (!queue.isEmpty()) {
			StmtDepth sd = queue.poll();
			Stmt stmt = sd.stmt;
			int depth = sd.depth;
			Logger.getLogger().finest(String.format("Process %s with depth %d", stmt, depth));
			if (stmt2Depth.containsKey(stmt) && depth >= stmt2Depth.get(stmt)) {
				continue;
			}
			else {
				stmt2Depth.put(stmt, depth);
			}
			for (Unit succ: icfg.getSuccsOf(stmt)) {
				traverseBFS(queue, (Stmt)succ, depth);
			}
		}
		Logger.getLogger().info("End BFS");
		
		// dump
		List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
		for (SootClass sc: lsc) {
			List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
			for (SootMethod sm: lsm) {
				try {
					PatchingChain<Unit> units = sm.retrieveActiveBody().getUnits();
					Logger.getLogger().finest(sm.toString());
					for (Unit u: units) {
						Logger.getLogger().finest(String.format("\t%s %s", stmt2Depth.get(u), u));
					}
				}
				catch (Exception e) {}
			}
		}
	}
	
	void initBFS(Queue<StmtDepth> queue) {
		// ICC entries
		SootMethod dummyMain = Scene.v().getMainMethod();
		PatchingChain<Unit> dummyMainUnits = dummyMain.retrieveActiveBody().getUnits();
		for (Unit u: dummyMainUnits) {
			Stmt s = (Stmt) u;
			if (s.containsInvokeExpr()) {
				for (SootMethod callee: icfg.getCalleesOfCallAt(s)) {
					queue.add(new StmtDepth((Stmt)callee.retrieveActiveBody().getUnits().getFirst(), 0));
				}
			}
		}
		
		// ICC branches
		List<SootClass> lsc = new ArrayList<SootClass>(Scene.v().getClasses());
		for (SootClass sc: lsc) {
			List<SootMethod> lsm = new ArrayList<SootMethod>(sc.getMethods());
			for (SootMethod sm: lsm) {
				try {
					PatchingChain<Unit> units = sm.retrieveActiveBody().getUnits();
					for (Unit u: units) {
						if (u instanceof IfStmt) {
							if (relationDecider.branchResult((IfStmt) u)) {
								queue.add(new StmtDepth((Stmt)u, 0));
								Logger.getLogger().fine(String.format("ICC decidable Branch %s in %s", u, sm));
							}
							else {
								Logger.getLogger().fine(String.format("ICC non-decidable Branch %s in %s", u, sm));
							}
						}
						else if (u instanceof SwitchStmt) {
							if (relationDecider.switchResult((SwitchStmt) u)) {
								queue.add(new StmtDepth((Stmt)u, 0));
							}
						}
					}
				}
				catch (Exception e) {}
			}
		}
	}
	
	void traverseBFS(Queue<StmtDepth> queue, Stmt stmt, int depth) {
		Stmt it = stmt;
		while (true) {
			if (!stmt2Depth.containsKey(it) || depth < stmt2Depth.get(it)) {
				if (!(it instanceof IfStmt || it instanceof SwitchStmt))
					stmt2Depth.put(it, depth);
			}
			
			if (it instanceof IfStmt || it instanceof SwitchStmt) {
				queue.add(new StmtDepth(it, depth+1));
				break;
			}
			else if (it.containsInvokeExpr()) {
				for (SootMethod callee: icfg.getCalleesOfCallAt(it)) {
					queue.add(new StmtDepth((Stmt)callee.retrieveActiveBody().getUnits().getFirst(), depth));
				}
			}
			else if (it instanceof ReturnStmt || it instanceof ReturnVoidStmt || it instanceof ThrowStmt) {
				break;
			}
			else if (it instanceof GotoStmt) {
				it = (Stmt)((GotoStmt)it).getTarget();
				continue;
			}
			// next
			if (icfg.getSuccsOf(it).size() != 1) {
				for (Unit succ: icfg.getSuccsOf(it)) {
					queue.add(new StmtDepth((Stmt)succ, depth));
				}
				break;
			}
			it = (Stmt) icfg.getSuccsOf(it).get(0);
		}
	}
	
	
	@Override
	public boolean branchResult(IfStmt stmt) {
		//Logger.getLogger().finest(String.format("\t%s:%d", stmt, stmt2Depth.get(stmt)));
		return stmt2Depth.containsKey(stmt) && stmt2Depth.get(stmt) <= depth;
	}

	@Override
	public boolean switchResult(SwitchStmt stmt) {
		//Logger.getLogger().finest(String.format("\t%s:%d", stmt, stmt2Depth.get(stmt)));
		return stmt2Depth.containsKey(stmt) && stmt2Depth.get(stmt) <= depth;
	}

	@Override
	public boolean stmtResult(Stmt stmt) {
		return relationDecider.stmtResult(stmt) || stmt2Depth.containsKey(stmt) && stmt2Depth.get(stmt) <= depth;
	}
}
