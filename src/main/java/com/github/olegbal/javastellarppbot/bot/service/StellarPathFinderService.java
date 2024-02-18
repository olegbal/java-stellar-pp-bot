package com.github.olegbal.javastellarppbot.bot.service;

import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.StrictSendPathsRequestBuilder;
import org.stellar.sdk.responses.PathResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StellarPathFinderService {

    public List<PathResponse> find(Server server, Asset sourceAsset, String sourceAmount, Asset destinationAsset) throws IOException {
        StrictSendPathsRequestBuilder strictSendPathRequestBuilder = server.strictSendPaths();
        return strictSendPathRequestBuilder
                .sourceAsset(sourceAsset)
                .sourceAmount(sourceAmount)
                .destinationAssets(List.of(destinationAsset)
                ).execute().getRecords();
    }

    public List<PathResponse> findPathsForBothAssets(Server server, Asset asset1, Asset asset2, String amount) throws IOException {
        List<PathResponse> paths = new ArrayList<>();

        find(server, asset1, amount, asset2).stream()
                .min((path1, path2) -> new BigDecimal(path2.getDestinationAmount())
                        .compareTo(new BigDecimal(path1.getDestinationAmount())))
                .ifPresent(paths::add);

        find(server, asset2, amount, asset1).stream().min((path1, path2) -> new BigDecimal(path2.getDestinationAmount())
                        .compareTo(new BigDecimal(path1.getDestinationAmount())))
                .ifPresent(paths::add);

        return paths;
    }
}


