package libericc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import libericc.preevaluate.PreEvaluateInstrumenter;
import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public class Main {
	public static void main(String[] args) {
		Date start = new Date();

		// parse argument and set soot options
		boolean parseSuccess = CmdParser.parseArg(args);
		if (!parseSuccess) return;
		setOptions();
		
		// Add transformer based on option 
		if (Config.instrument==Config.Instrument.pre_evaluate) {
			// store the logger class from jar file to tmp dir
			// soot will load the logger class in tmp dir
			PreEvaluateInstrumenter.storePreEvaluateLogger();
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.libericc", new HisdroidPreEvaluateTransformer()));
		} 
		else {
			Config.loadIccLogs(Config.icclogPath);
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.libericc", new HisdroidAnalysisTransformer()));
		}
		
		// start soot
		Scene.v().loadNecessaryClasses();
		soot.Main.v().autoSetOptions();
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
		
		// time and memory usage
		Date finish = new Date();
		long t = finish.getTime() - start.getTime();
		Logger.getLogger().info(String.format("Total time: %d min %d sec", t/60000, t%60000/1000));
		
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		Logger.getLogger().info(String.format("Used Memory: %d MB", usedMemory/1024/1024));
	}
	
	static void setOptions() {
		Options.v().set_whole_program(true); // -w
		Options.v().set_allow_phantom_refs(true); // -allow-phantom-refs
		Options.v().set_ignore_resolution_errors(true); // -ire
		Options.v().set_src_prec(Options.src_prec_apk); // -src-prec apk
		Options.v().set_force_overwrite(true);
		Options.v().set_process_multiple_dex(true);
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
		List<String> processDir = new ArrayList<String>();
		processDir.add(Config.apkPath);
		if (Config.instrument==Config.Instrument.pre_evaluate) processDir.add("./tmp");
		Options.v().set_process_dir(processDir);
		// generate call graph after dummy main generation
		Options.v().setPhaseOption("cg", "enabled:false");
	}
}
