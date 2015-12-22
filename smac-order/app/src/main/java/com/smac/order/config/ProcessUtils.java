package com.smac.order.config;

import android.text.method.HideReturnsTransformationMethod;

import com.smac.order.string.LevenshteinDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Huynh Quang Thao on 12/17/15.
 */
public class ProcessUtils {
    static String[] numberStr = new String[]{"một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    static String[] numbers = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};

    static String[] products = new String[]{
            "nước suối",
            "coca",
            "táo",
            "cam",
            "nho",
            "chuối",
            "khoai tây",
            "ostar",
            "snack",
            "poca",
            "hảo hảo",
            "ô ma chi",
            "kẹo cao su",
            "bánh quy",
            "kẹo sô cô la",
            "cà phê",
            "trà nhài",
            "xà phòng tắm",
            "bột giặt",
            "dầu gội",
            "sữa tắm",
            "nước rửa tay khô",
            "sữa chua",
            "bim bim hành",
            "bánh gạo",
            "trà chanh ice tea",
            "giấy ăn",
            "cà rốt"
    };

    static Map<String, String> criticalString = new HashMap<String, String>();

    static Map<String, String> criticalWord = new HashMap<String, String>();

    static String[] blockWords = new String[]{"trái", "quả", "gói", "củ", "chai", "cuộn"};

    static {
        criticalWord.put("cafe", "cà phê");
        criticalWord.put("next", "snack");
        criticalWord.put("bokeh", "poca");
        criticalWord.put("bioma", "ba ô ma");

        criticalWord.put("mua", "1");
        criticalWord.put("hay", "2");
        criticalWord.put("bao", "3");
        criticalWord.put("muốn", "4");
        criticalWord.put("buôn", "4");
        criticalWord.put("nam", "5");
        criticalWord.put("làm", "5");
        criticalWord.put("sáu", "6");
        criticalWord.put("xấu", "6");
        criticalWord.put("xin", "9");

    }

    static {
        criticalString.put("bioma", "3 ô ma");
        criticalString.put("bao machi", "3 ô ma chi");
        criticalString.put("bên hàng", "bim bim hành");
        criticalString.put("với anh", "giấy ăn");
        criticalString.put("osaka", "ostar");
        criticalString.put("quay tay", "khoai tây");
        criticalString.put("remix", "snack");
    }



    public static List<Pair<Integer, String>> solve(String sentence) {
        //String test = "cho tôi ba con chó    bốn con mèo năm con dê";
        //String test = "cho tôi ba trà ngài bốn O'star năm con dê nui";

        if (sentence.contains("cho tôi")) {
            sentence = sentence.replace("cho tôi", "");
        }

        sentence = sentence.toLowerCase().trim();

        // fix critical string
        for (Map.Entry<String, String> entry : criticalString.entrySet()) {
            if (sentence.contains(entry.getKey())) {
                sentence = sentence.replace(entry.getKey(), entry.getValue());
            }
        }

        // remove unnecessary words
        for (String word : blockWords) {
            if (sentence.contains(word)) {
                sentence = sentence.replace(word, "");
            }
        }

        String[] words = sentence.split("\\s+");

        // change critical word
        for (int i = 0; i < words.length; i++) {
            String newWord = getCriticalWord(words[i]);
            words[i] = newWord;
        }

        // change numberStr to num
        for (int i = 0; i < words.length; i++) {
            String newWord = getNumber(words[i]);
            words[i] = newWord;
        }

        List<Pair<Integer, String>> calledProducts = new ArrayList<Pair<Integer, String>>();

        // will be updated in runtime
        int lasWordIndex = words.length - 1;
        int currentWordIndex = words.length - 1;
        while (currentWordIndex >= 0) {
            int num = isNumber(words[currentWordIndex]);
            if (num != -1) {
                String productName = getWord(words, currentWordIndex+1, lasWordIndex);
                // if in this sentence. there are product after number. create one
                if (productName != null) {
                    Pair<Integer, String> p = new Pair<Integer, String>(num, productName);
                    calledProducts.add(p);

                }
                // else. do nothing
                lasWordIndex = currentWordIndex-1;
            }
            currentWordIndex--;
        }

        // reverse for accurated see
        for (int i = 0; i < calledProducts.size() / 2; i++) {
            Pair<Integer, String> tmp = calledProducts.get(i);
            int secondIndex = calledProducts.size()-1-i;
            calledProducts.set(i, calledProducts.get(secondIndex));
            calledProducts.set(secondIndex, tmp);
        }

        /*// change critical word
        for (int i = 0; i < calledProducts.size(); i++) {
            Pair<Integer, String> p = calledProducts.get(i);
            p.second = getCriticalWord(p.second);
        }*/

        // find the nearest string that match the original.
        for (int i = 0; i < calledProducts.size(); i++) {
            Pair<Integer, String> p = calledProducts.get(i);
            p.second = getMostAccuratedWord(p.second);
        }

        System.out.println("result");
        for (int i = 0; i < calledProducts.size(); i++) {
            Pair<Integer, String> p = calledProducts.get(i);
            System.out.println(p.first + "." + p.second);
        }

        // remove not found word
        List<Pair<Integer, String>> res = new ArrayList<Pair<Integer, String>>();
        for (int i = 0; i < calledProducts.size(); i++) {
            if (calledProducts.get(i).second != null) {
                res.add(calledProducts.get(i));
            }
        }

        return res;
    }


    private static int isNumber(String text) {
        text = text.trim().toLowerCase();
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i].equals(text)) {
                return i+1;
            }
        }
        return -1;
    }

    private static String getWord(String[] word, int from, int to) {
        String res = "";
        if (from > to) return null;
        if (to >= word.length) return null;

        for (int i = from; i <= to; i++) {
            res = res + word[i] + " ";
        }
        res = res.trim();
        return res;
    }

    private static String getCriticalWord(String word) {
        for (Map.Entry<String, String> entry : criticalWord.entrySet()) {
            if (entry.getKey().equals(word)) {
                return entry.getValue();
            }
        }
        return word;
    }

    private static String getNumber(String word) {
        for (int i = 0; i < numberStr.length; i++) {
            if (numberStr[i].equals(word)) {
                return numbers[i];
            }
        }
        return word;
    }

    private static String getMostAccuratedWord(String word) {
        int minDiff = Integer.MAX_VALUE;
        String res = null;
        for (String product : products) {
            int distance = LevenshteinDistance.computeDistance(product, word);
            if (distance < minDiff) {
                minDiff = distance;
                res = product;
            }
        }

        return res;
    }
}
