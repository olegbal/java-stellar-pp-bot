package com.github.olegbal.javastellarppbot.repository;


import com.github.olegbal.javastellarppbot.domain.PPStatisticRecord;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PPStatisticsRepository extends MongoRepository<PPStatisticRecord, ObjectId> {
}
