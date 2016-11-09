package libericc.relation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import libericc.Logger;

public class CmdParser {
	static boolean parseArg(String[] args) {
		Options options = new Options();
		// Apk file to analysis (required)
		options.addOption(
				Option.builder("a").longOpt("apk")
				.hasArg().argName("apkfile")
				.desc("Application to analyze")
				.required()
				.build());
		// Android jars (required)
		options.addOption(
				Option.builder("j").longOpt("android-jar")
				.hasArg().argName("jarpath")
				.desc("Android jar libraries")
				.required()
				.build());
		
		// The output format
		options.addOption(
				Option.builder("f").longOpt("output-format")
				.hasArg().argName("format")
				.desc("Output Format [none, apk, jimple] (Default: apk)")
				.build());

		// instrument depth
		options.addOption(
				Option.builder("d").longOpt("instrument-depth")
				.hasArg().argName("depth")
				.desc("Instrument depth")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			// required arguments
			Config.apkPath = line.getOptionValue("a");
			Logger.getLogger().config(String.format("apkPath = %s", Config.apkPath));
			Config.androidjars = line.getOptionValue("j");
			Logger.getLogger().config(String.format("androidjars = %s", Config.androidjars));
			// The output format
			if (line.hasOption("f")) {
				switch (line.getOptionValue("f")) {
				case "none":
					Config.outputFormat = Config.OutputFormat.none;
					break;
				case "apk":
					Config.outputFormat = Config.OutputFormat.apk;
					break;
				case "jimple":
					Config.outputFormat = Config.OutputFormat.jimple;
					break;
				default:
					throw new ParseException("Unknown argument in -f");
				}
				Logger.getLogger().config("outputFormat = "+line.getOptionValue("f"));
			}
			// instrument depth
			if (line.hasOption("d")) {
				Config.depth = Integer.parseInt(line.getOptionValue("d"));
			}
		}
		catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("libericc", options);
			return false;
		}
		return true;
	}
}
