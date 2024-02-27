package com.github.olegbal.javastellarppbot;

import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

public class AssetUtils {

    public static boolean isNative(Asset asset) {
        return asset.getType().equals("native");
    }

    public static String getCode(Asset asset) {
        return isNative(asset) ? "XLM" : ((AssetTypeCreditAlphaNum) asset).getCode();
    }

}
