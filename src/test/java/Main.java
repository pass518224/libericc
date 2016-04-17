

import java.util.Collections;
import java.util.Map;

import hisdroid.Analyzer;
import hisdroid.Config;
import hisdroid.DummyMainCreator;
import hisdroid.IDEAnalyzer;
import hisdroid.Pruner;
import soot.PackManager;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;


public class Main {	
	public static void main(String[] args) {
		setOptions();
		
		Config.loadIccLogs("/home/chchao/test1.json");
		final SootMethod dummyMain = new DummyMainCreator().createDummyMain();
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.IDEAnalysis", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				
				Analyzer analyzer = new IDEAnalyzer();
				analyzer.analyze(dummyMain);
				
				Pruner pruner = new Pruner(analyzer);
				pruner.prune();//*/
			}
		}));
		
		soot.Main.main(args);
	}
	
	static void setOptions() {
		Options.v().set_whole_program(true); // -w
		Options.v().set_allow_phantom_refs(true); // -allow-phantom-refs
		Options.v().set_ignore_resolution_errors(true); // -ire
		Options.v().set_src_prec(Options.src_prec_apk); // -src-prec apk
		Options.v().set_output_format(Options.output_format_dex); // -f dex
		//Options.v().set_output_format(Options.output_format_jimple); // -f J
		//  -android-jars /home/chchao/test/android-platforms-master -process-path /home/chchao/AndroidStudioProjects/Asdf/app/app-release.apk
		Options.v().set_android_jars("/home/chchao/test/android-platforms-master");
		Options.v().set_process_dir(Collections.singletonList("/home/chchao/AndroidStudioProjects/Asdf/app/app-release.apk"));
		
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
	}
}
