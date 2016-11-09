package libericc.relation;

import java.util.ArrayList;
import java.util.List;

import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

public class Main {
	
	public static void main(String[] args) {
		boolean parseSuccess = CmdParser.parseArg(args);
		if (!parseSuccess) return;
		setOptions();

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.libericc", new AnalysisTransformer()));
		
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
		Options.v().set_android_jars(Config.androidjars);
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

		List<String> processDir = new ArrayList<String>();
		processDir.add(Config.apkPath);
		Options.v().set_process_dir(processDir);
		Options.v().setPhaseOption("cg", "enabled:false");
	}
}
