package hisdroid.instrumenter;

import java.util.Collections;
import java.util.List;

import hisdroid.Analyzer;
import hisdroid.TriLogic;
import soot.PatchingChain;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

public class LogOnly extends Instrumenter {

	public LogOnly(Analyzer analyzer){
		super(analyzer);
	}
	
	@Override
	void instrumentBranch(PatchingChain<Unit> unitChain, IfStmt stmt, TriLogic result) {
		return;
	}

	@Override
	public List<Unit> instrumentSwitch(PatchingChain<Unit> unitChain, SwitchStmt stmt, ResultOfSwitch result) {
		return Collections.singletonList((Unit)stmt);
	}

}
