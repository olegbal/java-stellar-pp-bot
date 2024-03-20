package com.github.olegbal.javastellarppbot.domain;


import com.github.olegbal.javastellarppbot.bot.utils.PPOpType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "pptransactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PPStatisticRecord {
    @Id
    private ObjectId id;
    private String hash;
    private String sourceAssetCode;
    private String sourcePubKey;
    private BigDecimal sourceAmount;
    private String destAssetCode;
    private String destPubKey;
    private BigDecimal destAmount;
    private String path;
    private PPOpType opType;
    private boolean succeed;
    private String opCode;
    private String instanceName;
    private LocalDateTime dateTime;
}
