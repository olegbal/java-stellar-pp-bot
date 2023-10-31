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

    private final List<Server> horizonServers;
    private static int HORIZON_INDEX = 0;

    public StellarPPBotScheduler(List<Server> horizonServers) {
        this.horizonServers = horizonServers;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedRate = 10)
    public void stellarBotStarter() {
        try {
            Server server = horizonServers.get(HORIZON_INDEX);

            StrictSendPathsRequestBuilder xlmStrictSendPath = server.strictSendPaths();
            StrictSendPathsRequestBuilder btcStrictSendPath = server.strictSendPaths();
            StrictSendPathsRequestBuilder ethStrictSendPath = server.strictSendPaths();
            StrictSendPathsRequestBuilder usdcStrictSendPath = server.strictSendPaths();

            Asset yxlmAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(), "yXLM", "GARDNV3Q7YGT4AKSDF25LT32YSCCW4EV22Y2TV3I2PU2MMXJTEDL5T55"
            );
            Asset btcAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(), "BTC", "GDPJALI4AZKUU2W426U5WKMAT6CN3AJRPIIRYR2YM54TL2GDWO5O2MZM");

            Asset yBtcAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(), "yBTC", "GBUVRNH4RW4VLHP4C5MOF46RRIRZLAVHYGX45MVSTKA2F6TMR7E7L6NW"
            );

            Asset ethAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(),
                    "ETH",
                    "GBFXOHVAS43OIWNIO7XLRJAHT3BICFEIKOJLZVXNT572MISM4CMGSOCC"
            );

            Asset yethAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(),
                    "yETH",
                    "GDYQNEF2UWTK4L6HITMT53MZ6F5QWO3Q4UVE6SCGC4OMEQIZQQDERQFD"
            );

            Asset usdcAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(),
                    "USDC",
                    "GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
            );

            Asset yusdcAsset = Asset.create(
                    AssetType.ASSET_TYPE_CREDIT_ALPHANUM12.name(),
                    "yUSDC",
                    "GDGTVWSM4MGS4T7Z6W4RPWOCHE2I6RDFCIFZGS3DOA63LWQTRNZNTTFF"
            );

            ArrayList<PathResponse> xlmPaths = xlmStrictSendPath
                    .sourceAsset(new AssetTypeNative())
                    .sourceAmount("100")
                    .destinationAssets(List.of(yxlmAsset)
                    ).execute().getRecords();

            ArrayList<PathResponse> btcPaths = btcStrictSendPath
                    .sourceAsset(btcAsset)
                    .sourceAmount("0.0004")
                    .destinationAssets(List.of(yBtcAsset))
                    .execute().getRecords();

            ArrayList<PathResponse> ethPaths = ethStrictSendPath
                    .sourceAsset(ethAsset)
                    .sourceAmount("0.006")
                    .destinationAssets(List.of(yethAsset))
                    .execute().getRecords();

//            ArrayList<PathResponse> usdcPaths = usdcStrictSendPath
//                    .sourceAsset(usdcAsset)
//                    .sourceAmount("10")
//                    .destinationAssets(List.of(yusdcAsset))
//                    .execute().getRecords();

            List<PathResponse> list = Stream.of(xlmPaths, btcPaths, ethPaths)
                    .flatMap(List::stream)
                    .filter(pathResponse -> {
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
                        }

                        return true;
                    }).toList();

            HORIZON_INDEX = HORIZON_INDEX == 1 ? 0 : 1;
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
