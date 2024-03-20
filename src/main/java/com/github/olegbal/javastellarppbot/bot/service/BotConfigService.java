package com.github.olegbal.javastellarppbot.bot.service;

import com.github.olegbal.javastellarppbot.domain.config.AssetData;
import com.github.olegbal.javastellarppbot.domain.config.StableAssetGroupConfig;
import com.github.olegbal.javastellarppbot.domain.config.StableAssetsPathPaymentConfig;
import com.github.olegbal.javastellarppbot.repository.StableAssetConfigRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.stellar.sdk.xdr.AssetType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Data
@Slf4j
@Service
public class BotConfigService {

    @Value("${payment.base-fee}")
    private long baseFee;

    @Value("${payment.profit-percent}")
    private double profitPercentage;

    @Value("${payment.timeout-seconds}")
    private int paymentTimeout;

    @Value("${bot.name}")
    private String botName;

    private final StableAssetConfigRepository repository;

    public BotConfigService(StableAssetConfigRepository repository) {
        this.repository = repository;
    }

    public Optional<StableAssetsPathPaymentConfig> loadStableAssetConfig() {
        return repository.findById(this.botName);
    }

    public Optional<StableAssetsPathPaymentConfig> loadStableAssetConfig(String botName) {
        return repository.findById(botName);
    }

    public StableAssetsPathPaymentConfig getDefaultConfig() {

        StableAssetsPathPaymentConfig defaultConfig = repository.findById("default").orElseGet(() -> {
            String name = "default-mock";
            StableAssetsPathPaymentConfig defaultStablePPConfig = new StableAssetsPathPaymentConfig();

            StableAssetGroupConfig stableAssetGroupConfig = new StableAssetGroupConfig();
            AssetData assetGroup11 = new AssetData(null, null, AssetType.ASSET_TYPE_NATIVE);
            AssetData assetGroup12 = new AssetData("yXLM", "GARDNV3Q7YGT4AKSDF25LT32YSCCW4EV22Y2TV3I2PU2MMXJTEDL5T55", AssetType.ASSET_TYPE_CREDIT_ALPHANUM4);
            stableAssetGroupConfig.setStableAssetPair(Pair.of(assetGroup11, assetGroup12));
            stableAssetGroupConfig.setReservedAmount(BigDecimal.valueOf(30));
            stableAssetGroupConfig.setOperationAmount(BigDecimal.valueOf(1));
            stableAssetGroupConfig.setKey("yXLM");

            defaultStablePPConfig.setBotName(name);
            defaultStablePPConfig.setStableAssetGroups(List.of(stableAssetGroupConfig));
            return defaultStablePPConfig;
        });


        log.warn("Default config loaded for instance {}", defaultConfig.botName);


        return defaultConfig;
    }
}
