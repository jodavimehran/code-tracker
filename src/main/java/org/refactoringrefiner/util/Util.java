package org.refactoringrefiner.util;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.util.Hashing;
import org.refactoringrefiner.element.Method;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Util {
    private Util() {
    }

    public static String getPath(String filePath, String className) {
        try {
            CharSequence charSequence = longestSubstring(filePath.substring(0, filePath.lastIndexOf("/")), className.replace(".", "/"));
            String srcFile = filePath.toLowerCase().replace(charSequence, "$");
            srcFile = srcFile.substring(0, srcFile.lastIndexOf("$"));
            return srcFile;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
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

    public static String getIdentifierExcludeVersion(UMLOperation info, boolean containsBody, boolean containsDocumentation) {
        StringBuilder sb = new StringBuilder();
        sb.append(getPath(info.getLocationInfo().getFilePath(), info.getClassName()));
        sb.append(info.getClassName());
        sb.append(String.format("#(%s)", info.getVisibility()));

        Set<String> modifiers = new TreeSet<>();
        if (info.isStatic())
            modifiers.add("static");
        if (info.isAbstract())
            modifiers.add("abstract");
        if (info.isFinal())
            modifiers.add("final");

        if (info.isSynchronized())
            modifiers.add("synchronized");

        if (!modifiers.isEmpty()) {
            sb.append(String.format("(%s)", String.join(",", modifiers)));
        }

        sb.append(info.getName());
        sb.append("(");
        sb.append(info.getParametersWithoutReturnType().stream().map(Method.MethodParameter::new).map(Objects::toString).collect(Collectors.joining(",")));
        sb.append(")");
        if (info.getReturnParameter() != null) {
            sb.append(":");
            sb.append(info.getReturnParameter());
        }
        if (!info.getThrownExceptionTypes().isEmpty()) {
            sb.append("[");
            sb.append(info.getThrownExceptionTypes().stream().map(Object::toString).collect(Collectors.joining(",")));
            sb.append("]");
        }
        if (containsBody && info.getBody() != null) {
            sb.append("{");
            sb.append(info.getBody().getSha512());
            sb.append("}");
        }
        if (containsDocumentation && !info.getComments().isEmpty()) {
            sb.append("{");
            sb.append(Hashing.getSHA512(info.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";"))));
            sb.append("}");
        }
        sb.append(annotationsToString(info.getAnnotations()));
        return sb.toString();
    }
}
