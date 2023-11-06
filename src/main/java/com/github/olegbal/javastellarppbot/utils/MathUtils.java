package com.github.olegbal.javastellarppbot.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

    public static BigDecimal percentageDifference(BigDecimal firstNumber, BigDecimal secondNumber) {
        BigDecimal difference = firstNumber.subtract(secondNumber);
        BigDecimal average = firstNumber.add(secondNumber).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        BigDecimal percentDifference = difference.divide(average, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return percentDifference.setScale(2, RoundingMode.HALF_UP);  //Set the scale to 2 decimal places.
    }
}
