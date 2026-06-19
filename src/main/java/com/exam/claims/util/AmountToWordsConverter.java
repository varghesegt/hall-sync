package com.exam.claims.util;

import java.math.BigDecimal;

/**
 * Converts numeric amounts to Indian English words.
 * Example: 5418 → "Rupees Five Thousand Four Hundred and Eighteen Only"
 */
public final class AmountToWordsConverter {

    private AmountToWordsConverter() {}

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Rupees Zero Only";
        }

        long rupees = amount.longValue();
        int paise = amount.subtract(new BigDecimal(rupees)).multiply(new BigDecimal(100)).intValue();

        StringBuilder sb = new StringBuilder("Rupees ");

        if (rupees == 0) {
            sb.append("Zero");
        } else {
            sb.append(convertToWords(rupees).trim());
        }

        if (paise > 0) {
            sb.append(" and ").append(convertToWords(paise).trim()).append(" Paise");
        }

        sb.append(" Only");
        return sb.toString();
    }

    /**
     * Converts a number to Indian format words.
     * Indian numbering: ones, tens, hundreds, thousands, lakhs, crores
     */
    private static String convertToWords(long number) {
        if (number == 0) return "";
        if (number < 0) return "Minus " + convertToWords(-number);

        StringBuilder words = new StringBuilder();

        // Crores (10,000,000+)
        if (number / 10000000 > 0) {
            words.append(convertToWords(number / 10000000)).append(" Crore ");
            number %= 10000000;
        }

        // Lakhs (100,000+)
        if (number / 100000 > 0) {
            words.append(convertToWords(number / 100000)).append(" Lakh ");
            number %= 100000;
        }

        // Thousands (1,000+)
        if (number / 1000 > 0) {
            words.append(convertToWords(number / 1000)).append(" Thousand ");
            number %= 1000;
        }

        // Hundreds
        if (number / 100 > 0) {
            words.append(ONES[(int)(number / 100)]).append(" Hundred ");
            number %= 100;
        }

        // Remaining (tens and ones)
        if (number > 0) {
            if (words.length() > 0) {
                words.append("and ");
            }
            if (number < 20) {
                words.append(ONES[(int)number]);
            } else {
                words.append(TENS[(int)(number / 10)]);
                if (number % 10 > 0) {
                    words.append(" ").append(ONES[(int)(number % 10)]);
                }
            }
        }

        return words.toString().trim();
    }

    public static String formatReceivedLine(BigDecimal amount) {
        long rupees = amount.longValue();
        String words = convert(amount);
        
        // words already has "Rupees " prefix and " Only" suffix
        return "Received Rs. " + rupees + " /- (" + words + ")";
    }
}
