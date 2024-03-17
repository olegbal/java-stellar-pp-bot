package com.github.olegbal.javastellarppbot.bot.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProfitUtils {

    public static ProfitDifference calculateDifference(BigDecimal firstNumber, BigDecimal secondNumber) {
        BigDecimal difference = firstNumber.subtract(secondNumber);
        BigDecimal fullPercent = BigDecimal.valueOf(100);
        BigDecimal sum = firstNumber.add(secondNumber);

        BigDecimal average = sum.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

        BigDecimal percentDifference = difference.divide(average, 10, RoundingMode.HALF_UP).multiply(fullPercent);


        return new ProfitDifference(percentDifference.doubleValue(), difference.abs());
    }
}
