package hisdroid;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CmdParser {
	static Logger logger = Logger.getLogger("HisDroid");
	
	static boolean parseArg(String[] args) {
		Options options = new Options();
		options.addOption(
				Option.builder("l").longOpt("log")
				.hasArg().argName("logfile")
				.desc("ICC Log in JSON format")
				.required()
				.build());
		options.addOption(
				Option.builder("a").longOpt("apk")
				.hasArg().argName("apkfile")
				.desc("Application to analyze")
				.required()
				.build());
		options.addOption(
				Option.builder("j").longOpt("android-jar")
				.hasArg().argName("jarpath")
				.desc("Android jar libraries")
				.required()
				.build());
		options.addOption(
				Option.builder().longOpt("not-prune")
				.desc("Do not prune output code")
				.build());
		options.addOption(
				Option.builder("f").longOpt("output-format")
				.hasArg().argName("format")
				.desc("Output Format [none, apk, jimple] (Default: apk)")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			Config.icclogPath = line.getOptionValue("l");
			logger.config(String.format("icclogPath = %s", Config.icclogPath));
			Config.apkPath = line.getOptionValue("a");
			logger.config(String.format("apkPath = %s", Config.apkPath));
			Config.androidjars = line.getOptionValue("j");
			logger.config(String.format("androidjars = %s", Config.androidjars));
			if (line.hasOption("not-prune")) {
				Config.prune = false;
				logger.config("prune = false");
			}
			switch (line.getOptionValue("f", "apk")) {
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
		}
		catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("hisdroid", options);
			return false;
		}
		return true;
	}
}
