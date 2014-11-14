package com.magratheadesign.streamstation;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ejntoo on 03/10/14.
 * Methods for downloading files
 */
public class DownloadUtils {
    public static String[] units = {"bytes", "KiB", "MiB", "GiB"};

    public static String formatSize(int bytes) {
        double bytesHolder = (double) bytes;
        int level = 0;
        while (bytesHolder > 1024.0 && level < units.length) {
            bytesHolder = bytesHolder / 1024.0;
            level++;
        }
        String stringSize = bytesHolder + "";
        String[] stringSizeArr = stringSize.split("\\.");
        if (stringSizeArr.length == 1) return stringSize + units[level];
        String decimals = (stringSizeArr[1].length() > 1) ? stringSizeArr[1].substring(0,1) : "";
        decimals = (decimals.equals("")) ? "" : "."+decimals;
        return stringSizeArr[0] + decimals + " " + units[level];
    }

    public static File getAlternativeName(File original) {
        /*
        like getAlternativeName, but adds '_2' or '_3' etc. if filename already ends with '_1'
         */
        String[] nameArr = original.getName().split("\\.");

        String extension = "";
        String filename = "";
        if (nameArr.length == 1) {
            filename = nameArr[0];
        } else { // file has extension
            extension = "." + nameArr[nameArr.length - 1];
            for (int i = 0; i < nameArr.length - 1; i++) filename += nameArr[i];
        }
        Pattern p = Pattern.compile("(.*)_(\\d+)");
        Matcher m = p.matcher(filename);
        if (!m.matches()) return new File(original.getParent() + File.separator + filename + "_1" + extension);
        String original_name = m.group(1);
        int index = Integer.parseInt(m.group(2)) + 1;
        return new File(original.getParent() + File.separator + original_name + "_" + index + extension);
    }
}
