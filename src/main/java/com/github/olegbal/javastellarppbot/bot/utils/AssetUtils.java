package com.github.olegbal.javastellarppbot.bot.utils;

import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

public class AssetUtils {

    public static boolean isNative(Asset asset) {
        return asset.getType().equals("native");
    }

    public static String getCode(Asset asset) {
        return isNative(asset) ? "XLM" : ((AssetTypeCreditAlphaNum) asset).getCode();
    }

    public static String getIssuer(Asset asset) {
        return isNative(asset) ? "" : ((AssetTypeCreditAlphaNum) asset).getIssuer();
    }

}
