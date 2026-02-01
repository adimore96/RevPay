package com.revpay.utils;

import java.util.Scanner;

public class ConsoleUtils {

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing screen fails, just print some newlines
            System.out.println("\n".repeat(50));
        }
    }

    public static void printHeader(String title) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }

    public static void printSuccess(String message) {
        System.out.println("\n✓ " + message);
    }

    public static void printError(String message) {
        System.out.println("\n✗ " + message);
    }

    public static void printInfo(String message) {
        System.out.println("\nℹ " + message);
    }

    public static void printWarning(String message) {
        System.out.println("\n⚠ " + message);
    }

    public static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    public static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    public static boolean readBoolean(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("yes") || input.equals("y")) {
                return true;
            } else if (input.equals("no") || input.equals("n")) {
                return false;
            } else {
                System.out.println("Please enter 'yes' or 'no'.");
            }
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public static void printTable(String[] headers, String[][] data) {
        // Calculate column widths
        int[] colWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = headers[i].length();
        }

        for (String[] row : data) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null && row[i].length() > colWidths[i]) {
                    colWidths[i] = row[i].length();
                }
            }
        }

        // Print header
        printTableRow(headers, colWidths);

        // Print separator
        System.out.print("+");
        for (int width : colWidths) {
            System.out.print("-".repeat(width + 2) + "+");
        }
        System.out.println();

        // Print data rows
        for (String[] row : data) {
            printTableRow(row, colWidths);
        }
    }

    private static void printTableRow(String[] row, int[] colWidths) {
        System.out.print("|");
        for (int i = 0; i < row.length; i++) {
            String cell = row[i] != null ? row[i] : "";
            System.out.printf(" %-" + colWidths[i] + "s |", cell);
        }
        System.out.println();
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    public static String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    public static String formatPercentage(double percentage) {
        return String.format("%.2f%%", percentage);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}