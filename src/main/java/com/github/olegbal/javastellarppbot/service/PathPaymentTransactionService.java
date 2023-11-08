package com.github.olegbal.javastellarppbot.service;

import com.github.olegbal.javastellarppbot.bot.HorizonServerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.olegbal.javastellarppbot.utils.Constants.RESERVED_NATIVE_AMOUNT;

@Slf4j
@Service
public class PathPaymentTransactionService {

    private final AccountService accountService;
    private final HorizonServerManager horizonServerManager;

    public PathPaymentTransactionService(AccountService accountService, HorizonServerManager horizonServerManager) {
        this.accountService = accountService;
        this.horizonServerManager = horizonServerManager;
    }

    public void doStrictSend(Asset asset1, Asset asset2, BigDecimal sourceAmount, BigDecimal destAmount, BigDecimal profitPercentage) {
        Server server = horizonServerManager.getRelevantServer();

        List<Asset> assetsToFilter = List.of(asset1, asset2);

        AccountResponse account = null;
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
                    destAmount.toString()
            );

            TransactionBuilder tBuilder = new TransactionBuilder(account, Network.PUBLIC);
            Transaction transaction = tBuilder
                    .addOperation(ppSendBuilder.build())
                    .setBaseFee(10000)
                    .addPreconditions(
                            TransactionPreconditions
                                    .builder()
//                                    FIXME FIND PROPER VALUE.
                                    .timeBounds(TimeBounds.expiresAfter(5000))
                                    .build()
                    )
                    .build();

            transaction.sign(accountService.getKeyPair());

            try {
                SubmitTransactionResponse response = server.submitTransaction(transaction);
                if (response.isSuccess()) {
                    log.info("Transaction passed! {}", response.getHash());
                } else {
                    log.info("Transaction failed without exceptions");
                }
            } catch (Exception e) {
                log.info("Transaction failed {}", e.getMessage());
            }
        }
    }

    private boolean isGreaterThanReserve(BigDecimal balance1, BigDecimal balance2) {
        return balance1.compareTo(RESERVED_NATIVE_AMOUNT) > 0 && balance2.compareTo(RESERVED_NATIVE_AMOUNT) > 0;
    }
}
