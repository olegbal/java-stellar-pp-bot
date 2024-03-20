package com.github.olegbal.javastellarppbot.domain.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "stable_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StableAssetsPathPaymentConfig {
    @Id
    public String botName;
    List<StableAssetGroupConfig> stableAssetGroups;
}
