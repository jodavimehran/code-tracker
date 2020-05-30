package org.refactoringrefiner.element;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Version;

public abstract class BaseCodeElement  implements CodeElement {

    private final Version version;

    public BaseCodeElement(Version version) {
        this.version = version;
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
        int maxlen = 0;
        int lastSubsBegin = 0;

        for (int i = 0; i < str1.length(); i++) {
            for (int j = 0; j < str2.length(); j++) {
                if (str1.charAt(i) == str2.charAt(j)) {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];

                    if (num[i][j] > maxlen) {
                        maxlen = num[i][j];
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

    protected String getPath(String filePath, String className) {
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

    protected String getPackage(String filePath, String className) {
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

    @Override
    public int compareTo(CodeElement o) {
        return this.getIdentifier().compareTo(o.getIdentifier());
    }

    @Override
    public Version getVersion() {
        return version;
    }
}
