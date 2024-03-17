package com.github.olegbal.javastellarppbot.bot.service;

import com.github.olegbal.javastellarppbot.bot.HorizonServerManager;
import com.github.olegbal.javastellarppbot.bot.utils.AssetUtils;
import com.github.olegbal.javastellarppbot.config.PaymentConfigService;
import com.github.olegbal.javastellarppbot.repository.PPOpType;
import com.github.olegbal.javastellarppbot.repository.PPStatisticRecord;
import com.github.olegbal.javastellarppbot.repository.PPStatisticsRepository;
import com.github.olegbal.javastellarppbot.repository.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.xdr.AlphaNum12;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getCode;
import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getIssuer;
import static com.github.olegbal.javastellarppbot.bot.utils.Constants.RESERVED_NATIVE_AMOUNT;

@Slf4j
@Service
public class PathPaymentTransactionService {

    private final AccountService accountService;
    private final HorizonServerManager horizonServerManager;
    private final PaymentConfigService paymentConfigService;
    private final PPStatisticsRepository statisticsRepository;

    public PathPaymentTransactionService(AccountService accountService, HorizonServerManager horizonServerManager, PaymentConfigService paymentConfigService, PPStatisticsRepository statisticsRepository) {
        this.accountService = accountService;
        this.horizonServerManager = horizonServerManager;
        this.paymentConfigService = paymentConfigService;
        this.statisticsRepository = statisticsRepository;
    }

    public void doStrictSend(Asset asset1, Asset asset2,
                             BigDecimal sourceAmount,
                             BigDecimal destAmount,
                             List<Asset> path,
                             PPOpType ppOpType) {
        Server server = horizonServerManager.getRelevantServer();
        log.info("Starting doStrictSend({}, {}, {}, {}, {})", asset1, asset2, sourceAmount, destAmount, path);

        List<Asset> assetsToFilter = List.of(asset1, asset2);

        AccountResponse account;
        try {
            account = accountService.getAccount();
        } catch (IOException e) {
            log.error("Unable to fetch account data");
            throw new RuntimeException(e);
        }

        Map<Asset, BigDecimal> assetToBalanceMap = Arrays.stream(account.getBalances())
                .filter(balance -> balance.getAsset().isPresent())
                .filter(balance -> assetsToFilter.contains(balance.getAsset().get()))
                .collect(Collectors.toMap(
                        balance -> balance.getAsset().get(),
                        balance -> new BigDecimal(balance.getBalance())
                ));

        BigDecimal asset1Balance = assetToBalanceMap.get(asset1);
        BigDecimal asset2Balance = assetToBalanceMap.get(asset2);

        if (isGreaterThanReserve(asset1Balance, asset2Balance)) {
            PathPaymentStrictSendOperation.Builder ppSendBuilder = new PathPaymentStrictSendOperation.Builder(
                    asset1,
                    sourceAmount.toString(),
                    account.getAccountId(),
                    asset2,
                    destAmount.toString())
                    .setPath(path.toArray(Asset[]::new));

            TransactionBuilder tBuilder = new TransactionBuilder(account, Network.PUBLIC);
            Transaction transaction = tBuilder
                    .addOperation(ppSendBuilder.build())
                    .setBaseFee(paymentConfigService.getBaseFee())
                    .addPreconditions(
                            TransactionPreconditions
                                    .builder()
                                    .timeBounds(TimeBounds.expiresAfter(paymentConfigService.getPaymentTimeout()))
                                    .build()
                    )
                    .build();

            transaction.sign(accountService.getKeyPair());

            try {
                SubmitTransactionResponse response = server.submitTransaction(transaction, true);
                if (response.isSuccess()) {
                    log.info("Transaction passed! {}", response.getHash());

                    statisticsRepository.insert(
                            new PPStatisticRecord(
                                    null,
                                    response.getHash(),
                                    getCode(asset1),
                                    getIssuer(asset1),
                                    sourceAmount,
                                    getCode(asset2),
                                    getIssuer(asset2),
                                    destAmount,
                                    PathUtils.buildStringPath(path, " -> "),
                                    PPOpType.PROFIT,
                                    true,
                                    ""));
                } else {
                    String opCodes = String.join(" ,", response.getExtras().getResultCodes().getOperationsResultCodes());
                    String txResultCode = response.getExtras().getResultCodes().getTransactionResultCode();
                    log.info("Transaction failed {}, {}", txResultCode, opCodes);

                    statisticsRepository.insert(
                            new PPStatisticRecord(
                                    null,
                                    response.getHash(),
                                    getCode(asset1),
                                    getIssuer(asset1),
                                    sourceAmount,
                                    getCode(asset2),
                                    getIssuer(asset2),
                                    destAmount,
                                    PathUtils.buildStringPath(path, " -> "),
                                    PPOpType.PROFIT,
                                    false,
                                    opCodes));
                }
            } catch (Exception e) {
                log.info("Transaction failed", e);
            }
        }
    }

    private boolean isGreaterThanReserve(BigDecimal balance1, BigDecimal balance2) {
        boolean b1GreaterThanReserve = balance1.compareTo(RESERVED_NATIVE_AMOUNT) > 0;
        boolean b2GreaterThanReserve = balance2.compareTo(RESERVED_NATIVE_AMOUNT) > 0;
        if(b1GreaterThanReserve && b2GreaterThanReserve) {
            return true;
        }
        else {
            log.warn("Balance of selected amount is lower than reserved value. Cancelling path payment... balance1 : {}, balance2: {} ", balance1, balance2);
            return false;
        }

    }
}
