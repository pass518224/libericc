package libericc.evaluatecoverage;

import java.util.ArrayList;
import java.util.List;

import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public class Main {
	public static String androidjars, apkPath;
	
	public static void main(String[] args) {
		apkPath = args[0];
		androidjars = args[1];
		setOptions();

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.libericc", new CoverageTransformer()));
		
		Scene.v().loadNecessaryClasses();
		soot.Main.v().autoSetOptions();
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
	}


	static void setOptions() {
		Options.v().set_whole_program(true); // -w
		Options.v().set_allow_phantom_refs(true); // -allow-phantom-refs
		Options.v().set_ignore_resolution_errors(true); // -ire
		Options.v().set_src_prec(Options.src_prec_apk); // -src-prec apk-c-j
		Options.v().set_force_overwrite(true);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_android_jars(androidjars);
		Options.v().set_output_format(Options.output_format_none); // -f n
		//Options.v().set_output_format(Options.output_format_jimple); // -f J
		//Options.v().set_output_format(Options.output_format_dex); // -f dex

		List<String> processDir = new ArrayList<String>();
		processDir.add(apkPath);
		Options.v().set_process_dir(processDir);
		Options.v().setPhaseOption("cg", "enabled:false");
	}
}
