package com.github.olegbal.javastellarppbot.domain.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StableAssetGroupConfig {
    private String key;
//    TODO ADD SUPPORT FOR MORE THAN 2 ASSETS
    private Pair<AssetData, AssetData> stableAssetPair;
    private BigDecimal reservedAmount;
    private BigDecimal operationAmount;
    private BigDecimal profitPercentage;
}
