package com.github.olegbal.javastellarppbot.bot.service;

import com.github.olegbal.javastellarppbot.bot.HorizonServerManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;

import java.io.IOException;

@Service
public class AccountService {

    @Value("${account.key:''}")
    private String accountKey;

    private final HorizonServerManager horizonServerManager;

    public AccountService(HorizonServerManager horizonServerManager) {
        this.horizonServerManager = horizonServerManager;
    }

    public AccountResponse getAccount() throws IOException {
        Server server = horizonServerManager.getRelevantServer();

        return server.accounts().account(getKeyPair().getAccountId());
    }

    public KeyPair getKeyPair() {
        return KeyPair.fromSecretSeed(accountKey);
    }

}
