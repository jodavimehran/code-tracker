package org.codetracker.blame.util;

import java.util.List;

/* Created by pourya on 2024-06-25*/
public class TabularPrint {
    static final boolean ENABLE_BORDERS = false;
    private static final int PADDING = 1;

    public static String make(List<String[]> data) {
        StringBuilder sb = new StringBuilder();
        if (data.isEmpty())
            return "No data to print";

        int[] columnWidths = calculateColumnWidths(data);

        sb.append(printRow(data.get(0), columnWidths, ENABLE_BORDERS));
        for (int i = 1; i < data.size(); i++) {
            sb.append(printRow(data.get(i), columnWidths, ENABLE_BORDERS));
        }
        return sb.toString();
    }

    private static String printRow(String[] row, int[] columnWidths, boolean showBorders) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            sb.append(String.format("%-" + (columnWidths[i] - 1) + "s", row[i]));
            if (showBorders) {
                sb.append(" | ");
            } else {
                sb.append(" ");
            }
        }
        String string = sb.toString();
        String trimmedString = string.replaceAll("\\s+$", "");
        return trimmedString + "\n";
    }

    private static void printSeparatorLine(int[] columnWidths, boolean showBorders) {
        StringBuilder sb = new StringBuilder();
        for (int width : columnWidths) {
            sb.append("+").append(getRepeat(width));
        }
        sb.append("+");
        if (showBorders) {
            System.out.println(sb.toString());
        }
    }

    private static String getRepeat(int width) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < width; i++) {
            result.append("-");
        }
        return result.toString();
    }

    private static int[] calculateColumnWidths(List<String[]> data) {
        int numColumns = data.get(0).length;
        int[] columnWidths = new int[numColumns];

        for (String[] row : data) {
            for (int i = 0; i < numColumns; i++) {
                columnWidths[i] = Math.max(columnWidths[i], row[i].length() + PADDING); // Add padding
            }
        }

        return columnWidths;
    }
}
