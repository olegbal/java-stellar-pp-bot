package com.github.olegbal.javastellarppbot.bot.service;

import com.github.olegbal.javastellarppbot.bot.HorizonServerManager;
import com.github.olegbal.javastellarppbot.bot.utils.PPOpType;
import com.github.olegbal.javastellarppbot.bot.utils.PathUtils;
import com.github.olegbal.javastellarppbot.domain.PPStatisticRecord;
import com.github.olegbal.javastellarppbot.domain.config.StableAssetGroupConfig;
import com.github.olegbal.javastellarppbot.repository.PPStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getCode;
import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getIssuer;

@Slf4j
@Service
public class PathPaymentTransactionService {

    private final AccountService accountService;
    private final HorizonServerManager horizonServerManager;
    private final PPStatisticsRepository statisticsRepository;
    private final BotConfigService botConfigService;

    public PathPaymentTransactionService(AccountService accountService, HorizonServerManager horizonServerManager,
                                         PPStatisticsRepository statisticsRepository, BotConfigService botConfigService) {
        this.accountService = accountService;
        this.horizonServerManager = horizonServerManager;
        this.statisticsRepository = statisticsRepository;
        this.botConfigService = botConfigService;
    }

    public void doStrictSend(Asset asset1,
                             Asset asset2,
                             BigDecimal sourceAmount,
                             BigDecimal destAmount,
                             List<Asset> path,
                             PPOpType ppOpType,
                             StableAssetGroupConfig stableAssetGroupConfig) {
        Server server = horizonServerManager.getRelevantServer();
        log.info("Starting doStrictSend({}, {}, {}, {}, {})", asset1, asset2, sourceAmount, destAmount, path);


        AccountResponse account = loadAccount();

        Optional<AccountResponse.Balance> assetBalanceOpt = Arrays.stream(account.getBalances())
                .filter(balance -> balance.getAsset().isPresent())
                .filter(balance -> asset1.equals(balance.getAsset().get())).findFirst();


        if (assetBalanceOpt.isPresent() && isGreaterThanReserve(assetBalanceOpt.get(), stableAssetGroupConfig)) {
            PathPaymentStrictSendOperation.Builder ppSendBuilder = new PathPaymentStrictSendOperation.Builder(
                    asset1,
                    sourceAmount.toString(),
                    account.getAccountId(),
                    asset2,
                    destAmount.toString())
                    .setPath(path.toArray(Asset[]::new));

            Transaction transaction = buildTransaction(account, ppSendBuilder);

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
                                    ppOpType,
                                    true,
                                    "tx_passed",
                                    botConfigService.getBotName(),
                                    LocalDateTime.now())
                    );
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
                                    ppOpType,
                                    false,
                                    opCodes,
                                    botConfigService.getBotName(),
                                    LocalDateTime.now())
                    );
                }
            } catch (Exception e) {
                log.info("Transaction failed", e);
            }
        }
    }

    private Transaction buildTransaction(AccountResponse account, PathPaymentStrictSendOperation.Builder ppSendBuilder) {
        TransactionBuilder transactionBuilder = new TransactionBuilder(account, Network.PUBLIC);

        return transactionBuilder
                .addOperation(ppSendBuilder.build())
                .setBaseFee(botConfigService.getBaseFee())
                .addPreconditions(buildPreconditions())
                .build();
    }

    private TransactionPreconditions buildPreconditions() {
        return TransactionPreconditions
                .builder()
                .timeBounds(TimeBounds.expiresAfter(botConfigService.getPaymentTimeout()))
                .build();
    }

    private AccountResponse loadAccount() {
        AccountResponse account;
        try {
            account = accountService.getAccount();
        } catch (IOException e) {
            log.error("Unable to fetch account data");
            throw new RuntimeException(e);
        }
        return account;
    }

    private boolean isGreaterThanReserve(AccountResponse.Balance balance, StableAssetGroupConfig config) {
        String assetBalanceString = balance.getBalance();
        BigDecimal assetBalance = new BigDecimal(assetBalanceString);
        boolean b1GreaterThanReserve = assetBalance.compareTo(config.getReservedAmount()) > 0;
        if (b1GreaterThanReserve) {
            return true;
        } else {
            String assetCode = balance.getAssetCode().orElse("");
            log.warn("Balance of selected asset {} is lower than reserved value. Cancelling path payment... balance : {}", assetBalanceString, assetCode);
            return false;
        }

    }
}
