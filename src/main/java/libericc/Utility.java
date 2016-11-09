package libericc;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Utility {
	/*
	 * get the intent from icc
	 */
	public static JSONObject iccToIntent(JSONObject icc) {
		try {
			String transactCode = icc.getString("Transact_code");
			switch (transactCode) {
			case "SCHEDULE_SEND_RESULT_TRANSACTION":
				return icc.getJSONObject("ri").getJSONObject("mData");
			case "SCHEDULE_SERVICE_ARGS_TRANSACTION":
				return icc.getJSONObject("args");
			case "SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION":
			case "SCHEDULE_BIND_SERVICE_TRANSACTION":
			case "SCHEDULE_UNBIND_SERVICE_TRANSACTION":
			case "SCHEDULE_RECEIVER_TRANSACTION":
				return icc.getJSONObject("intent");
			default:
				for (String key: icc.keySet()) {
					try {
						JSONObject obj = icc.getJSONObject(key);
						if (obj.has("Action")) {
							return obj;
						}
					}
					catch (Exception e) { continue; }
				}
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	static Map<Class, Integer> classToParcelTypeNumber = null;
	/*
	 * return the type number in Parcel
	 */
	@SuppressWarnings("rawtypes")
	public static int getParcelTypeNumber(Class type) {
		if (classToParcelTypeNumber == null) {
			classToParcelTypeNumber = new HashMap<Class, Integer>();
			
			classToParcelTypeNumber.put(String.class, 0);
			classToParcelTypeNumber.put(Integer.class, 1);
			classToParcelTypeNumber.put(Short.class, 5);
			classToParcelTypeNumber.put(Long.class, 6);
			classToParcelTypeNumber.put(Float.class, 7);
			classToParcelTypeNumber.put(Double.class, 8);
			classToParcelTypeNumber.put(Boolean.class, 9);
			classToParcelTypeNumber.put(Byte.class, 20);
		}
		return classToParcelTypeNumber.get(type);
	}
}
