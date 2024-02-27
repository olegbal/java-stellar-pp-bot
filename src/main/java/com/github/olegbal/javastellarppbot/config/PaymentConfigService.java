package com.github.olegbal.javastellarppbot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Data
@Service
public class PaymentConfigService {

    @Value("${payment.base-fee}")
    private long baseFee;

    @Value("${payment.profit-percent}")
    private int profitPercentage;

    @Value("${payment.timeout-seconds}")
    private int paymentTimeout;

}
