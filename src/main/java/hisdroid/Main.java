package hisdroid;

import java.util.Collections;
import java.util.Map;
import java.util.logging.LogManager;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.options.Options;


public class Main {	
	static String icclogPath = "/home/chchao/git/asdf/finder/logs/317.json"; //"/home/chchao/git/test/test2.json"
	static String apkPath = "/home/chchao/git/test/test1.apk";//"/home/chchao/AndroidStudioProjects/Asdf/app/app-release.apk";
	
	public static void main(String[] args) {
		readLogProperties();
		setOptions();
		Config.loadIccLogs(icclogPath);
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.IDEAnalysis", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				new DummyMainCreator().createDummyMain();

				PackManager.v().getPack("cg").apply();
				
				Analyzer analyzer = new IDEAnalyzer();
				analyzer.analyze(Scene.v().getMainMethod());
				
				Pruner pruner = new Pruner(analyzer);
				pruner.prune();//*/
			}
		}));
		
		soot.Main.main(new String[]{" "});
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
		Options.v().set_process_dir(Collections.singletonList(apkPath));
		
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
	}
	
	static void readLogProperties(){
		try {
			LogManager.getLogManager().readConfiguration(Object.class.getResourceAsStream("/javalog.properties"));
		}
		catch (Exception e) {
			System.err.println("Cannot Find Log Properties");
		}
	}
}
