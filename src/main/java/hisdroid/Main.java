package hisdroid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import hisdroid.instrumenter.StatsInstrumenter;
import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public class Main {
	static Logger logger = Logger.getLogger("HisDroid");

	public static void main(String[] args) {
		Date start = new Date();

		boolean parseSuccess = CmdParser.parseArg(args);
		if (!parseSuccess) return;
		readLogProperties();
		setOptions();
		Config.loadIccLogs(Config.icclogPath);
		if (Config.instrument==Config.Instrument.stats) {
			StatsInstrumenter.storeStatsCounter();
		}

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.hisdroid", new HisdroidAnalysisTransformer()));
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
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple); // -src-prec apk-c-j
		Options.v().set_force_overwrite(true);
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
		if (Config.instrument==Config.Instrument.stats) processDir.add("./tmp");
		Options.v().set_process_dir(processDir);
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
