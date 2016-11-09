package libericc.relation;

public class Config {
	public enum OutputFormat { none, jimple, apk; }
	public enum Instrument { none, prune;}

	public static Instrument instrument = Instrument.prune;
	public static OutputFormat outputFormat = OutputFormat.apk;
	public static String apkPath;
	public static String androidjars;
	public static int depth = 0;
}
