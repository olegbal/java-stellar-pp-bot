package com.github.olegbal.javastellarppbot.bot;

import com.github.olegbal.javastellarppbot.bot.service.PathPaymentTransactionService;
import com.github.olegbal.javastellarppbot.bot.service.StellarPathFinderService;
import com.github.olegbal.javastellarppbot.utils.Constants;
import com.github.olegbal.javastellarppbot.utils.ProfitDifference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.PathResponse;
import org.stellar.sdk.xdr.AssetType;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.olegbal.javastellarppbot.utils.ProfitUtils.calculateDifference;

@Service
@Slf4j
public class StellarPPBotScheduler {

    private final HorizonServerManager horizonServerManager;
    private final StellarPathFinderService stellarPathFinderService;
    private final PathPaymentTransactionService pathPaymentTransactionService;

    public StellarPPBotScheduler(HorizonServerManager horizonServerManager,
                                 StellarPathFinderService stellarPathFinderService,
                                 PathPaymentTransactionService pathPaymentTransactionService) {
        this.horizonServerManager = horizonServerManager;
        this.stellarPathFinderService = stellarPathFinderService;
        this.pathPaymentTransactionService = pathPaymentTransactionService;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedRate = 2)
    public void stellarBotStarter() {
        try {
            Server server = horizonServerManager.getRelevantServer();


            Asset yxmlAsset = Asset.create(AssetType.ASSET_TYPE_CREDIT_ALPHANUM4.name(),
                    "yXLM",
                    "GARDNV3Q7YGT4AKSDF25LT32YSCCW4EV22Y2TV3I2PU2MMXJTEDL5T55"
            );

            List<PathResponse> paths = stellarPathFinderService.findPathsForBothAssets(server,
                    new AssetTypeNative(), yxmlAsset, Constants.XML_OPERATION_AMOUNT);

            paths.forEach(pathResponse -> {
                BigDecimal sourceAmount = new BigDecimal(pathResponse.getSourceAmount());
                BigDecimal destinationAmount = new BigDecimal(pathResponse.getDestinationAmount());

                if (destinationAmount.compareTo(sourceAmount) > 0) {
                    ProfitDifference diff = calculateDifference(destinationAmount, sourceAmount);
                    if (diff.percents() > 1.0) {
                        log.info("NEW PROFIT TRIGGERED SOURCE ASSET: {}, SOURCE AMOUNT: {}, DEST: {}, DEST AMOUNT: {}",
                                pathResponse.getSourceAsset(),
                                pathResponse.getSourceAmount(),
                                pathResponse.getDestinationAsset(),
                                pathResponse.getDestinationAmount()
                        );

                        pathPaymentTransactionService.doStrictSend(
                                pathResponse.getSourceAsset(),
                                pathResponse.getDestinationAsset(),
                                sourceAmount,
                                sourceAmount);
                    }
                }
            });
        } catch (Exception e) {
            log.error("An error occured during executing scheduler");
        }

    }
}
