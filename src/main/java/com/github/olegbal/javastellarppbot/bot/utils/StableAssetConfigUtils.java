package com.github.olegbal.javastellarppbot.bot.utils;

import com.github.olegbal.javastellarppbot.domain.config.StableAssetGroupConfig;
import com.github.olegbal.javastellarppbot.domain.config.StableAssetsPathPaymentConfig;
import org.springframework.data.util.Pair;
import org.stellar.sdk.Asset;

import java.util.List;

import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getCode;

public class StableAssetConfigUtils {
    public static StableAssetGroupConfig findGroupConfig(StableAssetsPathPaymentConfig config, Pair<Asset, Asset> assetPair) {

        List<StableAssetGroupConfig> assetGroupConfig = config.getStableAssetGroups().stream()
                .filter(assetGroup -> assetGroup.getKey().equals(getCode(assetPair.getFirst()))
                        || assetGroup.getKey().equals(getCode(assetPair.getSecond())))
                .toList();

        if (assetGroupConfig.isEmpty()) {
            throw new RuntimeException("Failed to find config for provided assets");
        } else if (assetGroupConfig.size() > 1) {
            throw new RuntimeException("Configuration for provided assets is not unique");
        } else {
            return assetGroupConfig.get(0);
        }
    }
}
