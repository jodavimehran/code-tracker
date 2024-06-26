package org.codetracker.blame;

import java.util.List;

/* Created by pourya on 2024-06-25*/
public class TabularPrint {
    static final boolean ENABLE_BORDERS = false;
    private static final int PADDING = 1;

    public static void printTabularData(List<String[]> data) {
        // Assuming data is not null and has consistent length for each row
        if (data.isEmpty()) {
            System.out.println("No data to print.");
            return;
        }

        // Determine column widths based on data
        int[] columnWidths = calculateColumnWidths(data);

        // Print table header
//        printSeparatorLine(columnWidths, ENABLE_BORDERS);
        printRow(data.get(0), columnWidths, ENABLE_BORDERS);
//        printSeparatorLine(columnWidths, ENABLE_BORDERS);

        // Print table rows
        for (int i = 1; i < data.size(); i++) {
            printRow(data.get(i), columnWidths, ENABLE_BORDERS);
        }

        // Print bottom separator line
//        printSeparatorLine(columnWidths, ENABLE_BORDERS);
    }

    private static void printRow(String[] row, int[] columnWidths, boolean showBorders) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            sb.append(String.format("%-" + (columnWidths[i] - 1) + "s", row[i]));
            if (showBorders) {
                sb.append(" | ");
            } else {
                sb.append(" ");
            }
        }
        System.out.println(sb.toString());
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
