package hisdroid;

import java.util.Collections;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;


public class Main {	
	static String icclogPath;
	static String apkPath;
	static String androidjars;

	public static void main(String[] args) {
		readLogProperties();
		parseArg(args);
		setOptions();
		Config.loadIccLogs(icclogPath);

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

		soot.Main.main(new String[]{" "});
	}
	
	static void setOptions() {
		soot.options.Options.v().set_whole_program(true); // -w
		soot.options.Options.v().set_allow_phantom_refs(true); // -allow-phantom-refs
		soot.options.Options.v().set_ignore_resolution_errors(true); // -ire
		soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk); // -src-prec apk
		//soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple); // -src-prec apk-c-j
		//Options.v().set_output_format(Options.output_format_dex); // -f dex
		soot.options.Options.v().set_output_format(soot.options.Options.output_format_jimple); // -f J

		//  -android-jars /home/chchao/test/android-platforms-master -process-path /home/chchao/AndroidStudioProjects/Asdf/app/app-release.apk
		soot.options.Options.v().set_android_jars(androidjars);
		soot.options.Options.v().set_process_dir(Collections.singletonList(apkPath));
	}
	
	static void readLogProperties(){
		try {
			LogManager.getLogManager().readConfiguration(Object.class.getResourceAsStream("/javalog.properties"));
		}
		catch (Exception e) {
			System.err.println("Cannot Find Log Properties");
		}
	}
	
	static void parseArg(String[] args) {
		Options options = new Options();
		options.addOption(
				Option.builder("l").longOpt("log")
				.desc("ICC Log in JSON format")
				.hasArg().required()
				.build());
		options.addOption(
				Option.builder("a").longOpt("apk")
				.desc("Application")
				.hasArg().required()
				.build());
		options.addOption(
				Option.builder("j").longOpt("android-jar")
				.desc("Android jar libraries")
				.hasArg().required()
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			icclogPath = line.getOptionValue("l");
			apkPath = line.getOptionValue("a");
			androidjars = line.getOptionValue("j");
		}
		catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("hisdroid", options);
		}
	}
}
