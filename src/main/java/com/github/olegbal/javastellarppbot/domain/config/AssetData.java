package com.github.olegbal.javastellarppbot.domain.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.xdr.AssetType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetData {
    public String assetCode;
    public String assetIssuer;
    public AssetType assetType;

    public Asset toAsset() {
        return this.assetType == AssetType.ASSET_TYPE_NATIVE ? new AssetTypeNative()
                : Asset.create(assetType.name(), assetCode, assetIssuer);
    }
}
