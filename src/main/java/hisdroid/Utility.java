package hisdroid;

import org.json.JSONObject;

public class Utility {
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
				return icc.getJSONObject("intent");
			default:
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
	}
}
