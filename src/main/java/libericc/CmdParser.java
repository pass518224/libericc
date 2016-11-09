package libericc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/*
 * Parse command line options
 */

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
		
		// Instrument type
		options.addOption(
				Option.builder("i").longOpt("instrument")
				.hasArg().argName("type")
				.desc("Instrument type [none, prune, aggressive, pre-eva] (Default: prune)")
				.build());
		
		// The output format
		options.addOption(
				Option.builder("f").longOpt("output-format")
				.hasArg().argName("format")
				.desc("Output Format [none, apk, jimple] (Default: apk)")
				.build());
		
		// Runtime ICC log
		// pre-eva (pre evaluation) do not need this option
		options.addOption(
				Option.builder("l").longOpt("log")
				.hasArg().argName("logfile")
				.desc("ICC Log in JSON format")
				.build());
		
		// evaluate with ADB log
		// Generate ADB log by pre-evaluate apk 
		options.addOption(
				Option.builder("e").longOpt("evaluate-log")
				.hasArg().argName("adblog")
				.desc("evaluate accuracy with runtime adb log")
				.build());
		
		// Only evaluate an ICC  
		options.addOption(
				Option.builder("n").longOpt("iccno")
				.hasArg().argName("iccno")
				.desc("only process iccno")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			// required arguments
			Config.apkPath = line.getOptionValue("a");
			Logger.getLogger().config(String.format("apkPath = %s", Config.apkPath));
			Config.androidjars = line.getOptionValue("j");
			Logger.getLogger().config(String.format("androidjars = %s", Config.androidjars));
			// instrument type
			if (line.hasOption("i")) {
				switch (line.getOptionValue("i")) {
				case "none":
					Config.instrument = Config.Instrument.none;
					break;
				case "prune":
					Config.instrument = Config.Instrument.prune;
					break;
				case "aggressive":
					Config.instrument = Config.Instrument.aggressive;
					break;
				case "pre-eva":
					Config.instrument = Config.Instrument.pre_evaluate;
					break;
				default:
					throw new ParseException("Unknown argument in -i");
				}
				Logger.getLogger().config("instrument = "+line.getOptionValue("i"));
			}
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
			// Runtime ICC log
			if (line.hasOption("l")) {
				Config.icclogPath = line.getOptionValue("l");
				Logger.getLogger().config(String.format("icclogPath = %s", Config.icclogPath));
			}
			else if (Config.instrument != Config.Instrument.pre_evaluate) {
				throw new ParseException("-l is required");
			}
			// adblog for evaluation
			if (line.hasOption("e")) {
				Config.adblogPath = line.getOptionValue("e");
				Logger.getLogger().config(String.format("adblogPath = %s", Config.adblogPath));
			}
			// only process one ICC
			if (line.hasOption("n")) {
				Config.iccNo = Integer.parseInt(line.getOptionValue("n"));
				Logger.getLogger().config(String.format("Only process icc %s", Config.iccNo));
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
