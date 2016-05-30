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
				Option.builder("i").longOpt("instrument")
				.hasArg().argName("type")
				.desc("Instrument type [none, prune, stats, pre-evaluate] (Default: prune)")
				.build());
		options.addOption(
				Option.builder("f").longOpt("output-format")
				.hasArg().argName("format")
				.desc("Output Format [none, apk, jimple] (Default: apk)")
				.build());
		options.addOption(
				Option.builder("e").longOpt("evaluate")
				.hasArg().argName("adblog")
				.desc("evaluate accuracy with runtime adb log")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			Config.apkPath = line.getOptionValue("a");
			logger.config(String.format("apkPath = %s", Config.apkPath));
			Config.androidjars = line.getOptionValue("j");
			logger.config(String.format("androidjars = %s", Config.androidjars));
			if (line.hasOption("i")) {
				switch (line.getOptionValue("i")) {
				case "none":
					Config.instrument = Config.Instrument.none;
					break;
				case "prune":
					Config.instrument = Config.Instrument.prune;
					break;
				case "stats":
					Config.instrument = Config.Instrument.stats;
					break;
				case "pre-evaluate":
					Config.instrument = Config.Instrument.pre_evaluate;
					break;
				default:
					throw new ParseException("Unknown argument in -i");
				}
				logger.config("instrument = "+line.getOptionValue("i"));
			}
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
				logger.config("outputFormat = "+line.getOptionValue("f"));
			}
			if (line.hasOption("e")) {
				logger.config(String.format("adblogPath = %s", Config.adblogPath));
				Config.adblogPath = line.getOptionValue("e");
			}
			if (line.hasOption("l")) {
				logger.config(String.format("icclogPath = %s", Config.icclogPath));
				Config.icclogPath = line.getOptionValue("l");
			}
			else if (Config.instrument != Config.Instrument.pre_evaluate) {
				throw new ParseException("-l is required");
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
