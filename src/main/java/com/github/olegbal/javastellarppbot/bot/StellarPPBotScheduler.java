package com.github.olegbal.javastellarppbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.StrictSendPathsRequestBuilder;
import org.stellar.sdk.responses.PathResponse;
import org.stellar.sdk.xdr.AssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@Slf4j
public class StellarPPBotScheduler {

    private final HorizonServerManager horizonServerManager;

    public StellarPPBotScheduler(HorizonServerManager horizonServerManager) {
        this.horizonServerManager = horizonServerManager;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedRate = 10)
    public void stellarBotStarter() {
        try {
            Server server = horizonServerManager.getRelevantServer();
            log.info("SERVER URL {}" , server);

            StrictSendPathsRequestBuilder xlmStrictSendPath = server.strictSendPaths();

            Asset yxlmAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(), "yXLM", "GARDNV3Q7YGT4AKSDF25LT32YSCCW4EV22Y2TV3I2PU2MMXJTEDL5T55"
            );

            ArrayList<PathResponse> xlmPaths = xlmStrictSendPath
                    .sourceAsset(new AssetTypeNative())
                    .sourceAmount("1000")
                    .destinationAssets(List.of(yxlmAsset)
                    ).execute().getRecords();

            Stream.of(xlmPaths)
                    .flatMap(List::stream)
                    .forEach(pathResponse -> {
                        BigDecimal sourceAmount = new BigDecimal(pathResponse.getSourceAmount());
                        BigDecimal destinationAmount = new BigDecimal(pathResponse.getDestinationAmount());

                        if (destinationAmount.compareTo(sourceAmount) > 0) {
                            BigDecimal profitPercentage = percentageDifference(destinationAmount, sourceAmount);
                            if (profitPercentage.compareTo(new BigDecimal("0.5")) > -1) {
                                log.info("NEW PROFIT TRIGGERED SOURCE ASSET: {},SOURCE AMOUNT: {}, DEST: {}, DEST AMOUNT: {}",
                                        pathResponse.getSourceAsset(),
                                        pathResponse.getSourceAmount(),
                                        pathResponse.getDestinationAsset(),
                                        pathResponse.getDestinationAmount()
                                );
                            }
                        } else if (sourceAmount.compareTo(destinationAmount) > 0) {
                            BigDecimal profitPercentage = percentageDifference(sourceAmount, destinationAmount);
                            if (profitPercentage.compareTo(new BigDecimal("0.5")) > -1) {
                                log.info("NEW PROFIT TRIGGERED SOURCE ASSET: {},SOURCE AMOUNT: {}, DEST: {}, DEST AMOUNT: {}",
                                        pathResponse.getDestinationAsset(),
                                        pathResponse.getDestinationAmount(),
                                        pathResponse.getSourceAsset(),
                                        pathResponse.getSourceAmount()
                                );
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("An error occured during executing scheduler");
        }

    }

    public BigDecimal percentageDifference(BigDecimal firstNumber, BigDecimal secondNumber) {
        BigDecimal difference = firstNumber.subtract(secondNumber);
        BigDecimal average = firstNumber.add(secondNumber).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        BigDecimal percentDifference = difference.divide(average, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return percentDifference.setScale(2, RoundingMode.HALF_UP);  //Set the scale to 2 decimal places.
    }
}
