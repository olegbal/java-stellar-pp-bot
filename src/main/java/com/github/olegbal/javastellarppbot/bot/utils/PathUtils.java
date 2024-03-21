package com.github.olegbal.javastellarppbot.bot.utils;

import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.olegbal.javastellarppbot.bot.utils.AssetUtils.getCode;

public class PathUtils {

    public static String buildStringPath(List<Asset> paths, String delimeter) {
        return paths.stream()
                .map(AssetUtils::getCode)
                .collect(Collectors.joining(delimeter));
    }
}
