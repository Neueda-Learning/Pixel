package com.pixel.portfolio.util;

import java.util.Locale;
import java.util.Set;

/** Normalizes a raw Finnhub search "type" (e.g. "Common Stock", "ETP") into an app-level asset category. */
public final class AssetTypeClassifier {

    // Gold/silver are tracked via commodity-backed ETFs rather than raw spot/futures symbols.
    private static final Set<String> COMMODITY_SYMBOLS = Set.of(
            "GLD", "IAU", "SGOL", "GLDM", "AAAU", // gold
            "SLV", "SIVR", "PSLV" // silver
    );

    private AssetTypeClassifier() {}

    public static String classify(String symbol, String rawType) {
        String sym = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (COMMODITY_SYMBOLS.contains(sym)) return "COMMODITY";

        if (rawType == null || rawType.isBlank()) return "STOCK";
        String t = rawType.toLowerCase(Locale.ROOT);
        if (t.contains("mutual fund")) return "MUTUAL_FUND";
        if (t.contains("etf") || t.contains("etp")) return "ETF";
        if (t.contains("reit")) return "REIT";
        return "STOCK";
    }
}
