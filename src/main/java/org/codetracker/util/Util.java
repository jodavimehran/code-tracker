package org.codetracker.util;

import gr.uom.java.xmi.UMLAnnotation;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    private Util() {
    }

    public static String getPath(String filePath, String className) {
        String srcFile = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        CharSequence charSequence = longestSubstring(srcFile, className.replace(".", "/"));
        if (className.startsWith(charSequence.toString().replace("/", "."))) {
            srcFile = filePath.toLowerCase().replace(charSequence, "$");
            srcFile = srcFile.substring(0, srcFile.lastIndexOf("$"));
            return srcFile;
        }
        return srcFile;
    }

    public static String longestSubstring(String str1, String str2) {

        StringBuilder sb = new StringBuilder();
        if (str1 == null || str1.isEmpty() || str2 == null || str2.isEmpty())
            return "";

// ignore case
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

// java initializes them already with 0
        int[][] num = new int[str1.length()][str2.length()];
        int maxLength = 0;
        int lastSubsBegin = 0;

        for (int i = 0; i < str1.length(); i++) {
            for (int j = 0; j < str2.length(); j++) {
                if (str1.charAt(i) == str2.charAt(j)) {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];

                    if (num[i][j] > maxLength) {
                        maxLength = num[i][j];
                        // generate substring from str1 => i
                        int thisSubsBegin = i - num[i][j] + 1;
                        if (lastSubsBegin == thisSubsBegin) {
                            //if the current LCS is the same as the last time this block ran
                            sb.append(str1.charAt(i));
                        } else {
                            //this block resets the string builder if a different LCS is found
                            lastSubsBegin = thisSubsBegin;
                            sb = new StringBuilder();
                            sb.append(str1, lastSubsBegin, i + 1);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    protected static String getPackage(String filePath, String className) {
        try {
            String replace = className.replace(filePath.substring(filePath.lastIndexOf("/") + 1).replace(".java", ""), "$");
            String packageName = replace.substring(0, replace.lastIndexOf("$") - 1);
            packageName = getPath(filePath, className) + packageName;
            return packageName;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String annotationsToString(List<UMLAnnotation> umlAnnotations) {
        return umlAnnotations != null && !umlAnnotations.isEmpty()
                ? String.format("[%s]", umlAnnotations.stream().map(UMLAnnotation::toString).sorted().collect(Collectors.joining(";")))
                : "";
    }


    public static String getSHA512(String input) {
        String toReturn = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            toReturn = String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toReturn;
    }
}
