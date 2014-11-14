package com.magratheadesign.streamstation;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ejntoo on 03/10/14.
 * Utility methods for the HTTP server
 */
public class ServerUtils {
    public static String parseRequest (String method, String header, String body) {

        String[] lines = header.split("\\n");
        String requestLine = "";
        for (String s : lines) {
            if (s.toUpperCase().startsWith(method)) {
                requestLine = s;
                break;
            }
        }
        if (requestLine.equals("")) {
            return null;
        }
        Pattern p = Pattern.compile(method + "\\s/(\\S*)\\s.*");
        Matcher m = p.matcher(requestLine);
        if (m.matches()) {
            String result = m.group(1);
            if (method.equals("GET")) return result;
            return result + "?" + body;
        }
        return null;
    }

    public static String parseRequestLine (String input) {
        String[] arr = input.split("\\?");
        return (arr.length > 0) ? arr[0] : null;
    }

    public static HashMap<String, String> parseRequestParams (String input) {
        String[] arr = input.split("\\?");
        String rawMap = (arr.length > 1) ? arr[1] : null;
        if (rawMap == null) return null;

        HashMap<String, String> map = new HashMap<String, String>();
        String[] rawMapArr = rawMap.split("\\&");
        for (String entry: rawMapArr) {
            String[] entryArr = entry.split("=");
            try {
                String param = URLDecoder.decode(entryArr[1], "UTF-8");
                if (entryArr.length > 0) map.put(entryArr[0], param);
            } catch (UnsupportedEncodingException e) {
                Log.i("StreamStation", "Bad encoding key");
            }
        }
        return map;
    }

    public static BufferedInputStream getJsonDownloadList(Context context) {
        StringBuilder jsonData = new StringBuilder("{\"downloadlist\": {\n");
        ArrayList<AsyncDownload> downloads = ((StreamStationService) context).downloads;
        for (int i = 0; i < downloads.size(); i++) {
            AsyncDownload d = downloads.get(i);

            String status;
            if (d.canceled) status = "canceled";
            else if (d.progress == d.fileSize) status = "completed";
            else if (d.paused) status = "paused";
            else status = "downloading";

            jsonData.append("\"" + i + "\": ")
                .append("{")
                .append("\"name\": ")
                .append("\"" + d.fileName + "\", ")
                .append("\"size\": ")
                .append(d.fileSize + ", ")
                .append("\"progress\": ")
                .append(d.progress + ", ")
                .append("\"speed\": ")
                .append(d.speed + ", ")
                .append("\"readableSize\": ")
                .append("\"" + DownloadUtils.formatSize(d.fileSize) + "\", ")
                .append("\"readableSpeed\": ")
                .append("\"" + DownloadUtils.formatSize(d.speed) + "/s\", ")
                .append("\"readableProgress\": ")
                .append("\"" + DownloadUtils.formatSize(d.progress) + "\", ")
                .append("\"status\": ")
                .append("\"" + status + "\"")
                .append("}");
            if (i < downloads.size() - 1) jsonData.append(",");
            jsonData.append("\n");
        }
        jsonData.append("}}");

        InputStream is = new ByteArrayInputStream(jsonData.toString().getBytes());
        return new BufferedInputStream(is);
    }
    public static BufferedInputStream getActionResponse(HashMap<String, String> map) {
        String response = "<p>";
        if (map.containsKey("action")) {
            response += "Performing action " + map.get("action");
        }
        if (map.containsKey("id")) {
            response += " on element " + map.get("id");
        }
        response += "</p>";
        InputStream is = new ByteArrayInputStream(response.getBytes());
        return new BufferedInputStream(is);
    }
}
