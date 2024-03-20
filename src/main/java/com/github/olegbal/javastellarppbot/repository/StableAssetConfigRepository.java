package com.github.olegbal.javastellarppbot.repository;

import com.github.olegbal.javastellarppbot.domain.config.StableAssetsPathPaymentConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StableAssetConfigRepository extends MongoRepository<StableAssetsPathPaymentConfig, String> {
}
