package hisdroid;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.options.Options;

public class Main {
	static Logger logger = Logger.getLogger("HisDroid");

	public static void main(String[] args) {
		readLogProperties();
		boolean parseSuccess = CmdParser.parseArg(args);
		if (!parseSuccess) return;
		setOptions();
		Config.loadIccLogs(Config.icclogPath);

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.IDEAnalysis", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {

				new DummyMainCreator().createDummyMain();
				
				soot.options.Options.v().setPhaseOption("cg.spark", "enabled:true");
				soot.options.Options.v().setPhaseOption("cg.spark", "string-constants:true");
				PackManager.v().getPack("cg").apply();
				
				Analyzer analyzer = new IDEAnalyzer();
				analyzer.analyze(Scene.v().getMainMethod());
				
				Pruner pruner = new Pruner(analyzer);
				pruner.prune();//*/
			}
		}));
		
		Date start = new Date();
		
		Scene.v().loadNecessaryClasses();
		soot.Main.v().autoSetOptions();
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
		
		Date finish = new Date();
		long t = finish.getTime() - start.getTime();
		logger.info(String.format("Total time: %d min %d sec", t/60000, t%60000/1000));
	}
	
	static void setOptions() {
		Options.v().set_whole_program(true); // -w
		Options.v().set_allow_phantom_refs(true); // -allow-phantom-refs
		Options.v().set_ignore_resolution_errors(true); // -ire
		Options.v().set_src_prec(Options.src_prec_apk); // -src-prec apk
		switch (Config.outputFormat) {
		case none:
			Options.v().set_output_format(Options.output_format_none); // -f n
			break;
		case jimple:
			Options.v().set_output_format(Options.output_format_jimple); // -f J
			break;
		case apk:
		default:
			Options.v().set_output_format(Options.output_format_dex); // -f dex
		}

		Options.v().set_android_jars(Config.androidjars);
		Options.v().set_process_dir(Collections.singletonList(Config.apkPath));
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
