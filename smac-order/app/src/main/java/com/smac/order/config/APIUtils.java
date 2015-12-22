package com.smac.order.config;

import java.util.List;

/**
 * Created by Huynh Quang Thao on 12/18/15.
 */
public class APIUtils {
    public static String generateContent(List<Pair<Integer, String>> products) {
        String res = "";
        String productBody = generateBody(products);
        res = "{\n" +
                "    \"access_token\": \"096648c0-a4f1-11e5-a708-b73dceb94e66\",\n" +
                "    \"orders\": [" +
                productBody +
                "]\n" +
                "}";
        return res;
    }

    public static String generateBody(List<Pair<Integer, String>> orders) {
        String res = "";
        for (Pair<Integer, String> order : orders) {
            String str = "";
            str += "\"name\": \"" + order.second + "\"" + ",";
            str += "\"number\":" + order.first;
            str = "{" + str + "}" + ",";
            res += str;
        }

        if (res.length()  == 0) return "";

        if (res.charAt(res.length() - 1) == ',') {
            res = res.substring(0, res.length()-1);
        }

        return res;
    }
}
