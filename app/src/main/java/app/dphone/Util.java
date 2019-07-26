package app.dphone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;


public class Util {
    public static String StringifyJsonArrayAsList(ArrayList<JSONObject> jsonArrayAsList)
    {
        String stringifyResult = "[";
        for (int i = 0; i < jsonArrayAsList.size(); i++) {
            stringifyResult += jsonArrayAsList.get(i).toString();
            if (i < jsonArrayAsList.size() - 1) {
                stringifyResult += ",";
            }
        }
        stringifyResult += "]";

        return stringifyResult;
    }

    public static String StringifyJSONArray(JSONArray jsonArray){
        ArrayList<JSONObject> jsonArrayAsList = ConvertJSONArrayToArrayOfJSONObject(jsonArray);
        return StringifyJsonArrayAsList(jsonArrayAsList);
    }

    public static ArrayList<JSONObject> ConvertJSONArrayToArrayOfJSONObject(JSONArray jsonArray){
        ArrayList<JSONObject> jsonArrayAsList = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                jsonArrayAsList.add((JSONObject)jsonArray.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArrayAsList;
    }

    public static String GetElapsedTime(Date startDate, Date endDate){

        Duration diff = Duration.between(Instant.ofEpochMilli(startDate.getTime()), Instant.ofEpochMilli(endDate.getTime()));
        long minutes = diff.toMinutes();
        diff = diff.minusMinutes(minutes);
        long seconds = diff.getSeconds();
        return String.format("%02d", minutes )+ ":" + String.format("%02d", seconds);
    }
}
