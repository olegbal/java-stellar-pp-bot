package com.github.olegbal.javastellarppbot.bot;

import com.github.olegbal.javastellarppbot.bot.service.BotConfigService;
import com.github.olegbal.javastellarppbot.bot.service.PathPaymentTransactionService;
import com.github.olegbal.javastellarppbot.bot.service.StellarPathFinderService;
import com.github.olegbal.javastellarppbot.bot.utils.PPOpType;
import com.github.olegbal.javastellarppbot.bot.utils.PathUtils;
import com.github.olegbal.javastellarppbot.bot.utils.ProfitDifference;
import com.github.olegbal.javastellarppbot.domain.config.AssetData;
import com.github.olegbal.javastellarppbot.domain.config.StableAssetsPathPaymentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.PathResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getCode;
import static com.github.olegbal.javastellarppbot.bot.utils.ProfitUtils.calculateDifference;
import static com.github.olegbal.javastellarppbot.bot.utils.StableAssetConfigUtils.findGroupConfig;

@Service
@Slf4j
public class StellarPPBotScheduler {

    private final HorizonServerManager horizonServerManager;
    private final StellarPathFinderService stellarPathFinderService;
    private final PathPaymentTransactionService pathPaymentTransactionService;
    private final BotConfigService botConfigService;

    public StellarPPBotScheduler(HorizonServerManager horizonServerManager,
                                 StellarPathFinderService stellarPathFinderService,
                                 PathPaymentTransactionService pathPaymentTransactionService, BotConfigService botConfigService) {
        this.horizonServerManager = horizonServerManager;
        this.stellarPathFinderService = stellarPathFinderService;
        this.pathPaymentTransactionService = pathPaymentTransactionService;
        this.botConfigService = botConfigService;
    }

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedRate = 2)
    public void findProfit() {
        try {
            Server server = horizonServerManager.getRelevantServer();
            StableAssetsPathPaymentConfig globalConfig = botConfigService.loadStableAssetConfig()
                    .orElseGet(botConfigService::getDefaultConfig);

            List<PathResponse> paths = buildPaths(globalConfig, server);

            paths.forEach(pathResponse -> {
                String sourceAmountString = pathResponse.getSourceAmount();
                BigDecimal sourceAmount = new BigDecimal(sourceAmountString);
                String destAmountString = pathResponse.getDestinationAmount();
                BigDecimal destinationAmount = new BigDecimal(destAmountString);

                if (destinationAmount.compareTo(sourceAmount) > 0) {
                    ProfitDifference diff = calculateDifference(destinationAmount, sourceAmount);
                    if (diff.percents() > botConfigService.getProfitPercentage()) {
                        String pathString = PathUtils.buildStringPath(pathResponse.getPath(), " -> ");

                        Asset sourceAsset = pathResponse.getSourceAsset();
                        Asset destAsset = pathResponse.getDestinationAsset();
                        log.info("NEW PROFIT TRIGGERED SOURCE ASSET: {}, SOURCE AMOUNT: {}, DEST: {}, DEST AMOUNT: {}, PATH: {}",
                                sourceAsset,
                                sourceAmountString,
                                destAsset,
                                destAmountString,
                                String.join(" -> ", getCode(sourceAsset), pathString, getCode(destAsset))
                        );

                        BigDecimal halfOfDifference = diff.value().divide(new BigDecimal("2"), RoundingMode.HALF_UP);

                        pathPaymentTransactionService.doStrictSend(
                                sourceAsset,
                                destAsset,
                                sourceAmount,
                                sourceAmount.add(halfOfDifference),
                                pathResponse.getPath(),
                                PPOpType.PROFIT,
                                findGroupConfig(globalConfig, Pair.of(sourceAsset, destAsset))
                        );
                    }
                }
            });
        } catch (Exception e) {
            log.error("An error occured during executing scheduler", e);
        }

    }

    private List<PathResponse> buildPaths(StableAssetsPathPaymentConfig config, Server server) {
        List<PathResponse> paths = new ArrayList<>();

        config.getStableAssetGroups()
                .forEach(stableGroup -> {
                    try {
                        Pair<AssetData, AssetData> assetPair = stableGroup.getStableAssetPair();
                        List<PathResponse> groupPaths = stellarPathFinderService.findPathsForBothAssets(server,
                                assetPair.getFirst().toAsset(),
                                assetPair.getSecond().toAsset(),
                                stableGroup.getOperationAmount().toString()
                        );
                        paths.addAll(groupPaths);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        return paths;
    }
}
