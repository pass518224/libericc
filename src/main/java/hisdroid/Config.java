package hisdroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.JSONArray;
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
		iccPreprocess();
	}
	
	static void iccPreprocess() {
		for (String key: iccLogs.keySet()) {
			try {
				JSONObject map = new JSONObject();
				JSONObject intent = Utility.iccToIntent(iccLogs.getJSONObject(key));
				JSONObject bundle = intent.getJSONObject("mExtras");
				
				JSONArray arr = bundle.getJSONArray("ArrayMap");
				for (int i=0; i<arr.length(); i++) {
					JSONObject a = arr.getJSONObject(i);
					map.put(a.getString("key"), a);
				}
				bundle.put("map", map);
			}
			catch (Exception e) {}
		}
	}
	
	static public JSONObject getIccLogs(){
		return iccLogs;
	}
}
