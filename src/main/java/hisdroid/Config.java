package hisdroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.JSONObject;

public class Config {
	static JSONObject iccLogs;
	
	static public void loadIccLogs(String filename){
		String content = null;
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
			content = s.useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			s.close();
		}
		iccLogs = new JSONObject(content);
	}
	
	static public JSONObject getIccLogs(){
		return iccLogs;
	}
}
