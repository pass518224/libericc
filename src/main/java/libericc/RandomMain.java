package libericc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

/*
 * An entry point of evaluate random guessing
 * takes (apk, androidjars, adblog) as argument
 */

public class RandomMain {
	static String androidjars;
	static String apkPath;
	
	public static void main(String[] args) {
		Date start = new Date();

		// parse argument and set soot options
		apkPath = args[0];
		androidjars = args[1];
		Config.adblogPath = args[2];
		setOptions();
		
		// Add transformer based on option 
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.libericc", new RandomTransformer()));
		
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
		Options.v().set_output_format(Options.output_format_none); // -f n

		Options.v().set_android_jars(androidjars);
		List<String> processDir = new ArrayList<String>();
		processDir.add(apkPath);
		Options.v().set_process_dir(processDir);
	}
}
