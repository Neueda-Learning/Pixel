package com.pixel.portfolio.loader;

import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Bulk-loads historical daily prices from CSVs in the seed folder on startup.
 * Falls back to a synthetic random walk for a demo symbol set if no CSVs are
 * present and price_history is empty, so the app is never blank for a demo.
 */
@Component
public class HistoricalDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataLoader.class);

    private static final Map<String, String[]> KNOWN_INSTRUMENTS = Map.ofEntries(
            Map.entry("AAPL", new String[]{"Apple Inc.", "STOCK"}),
            Map.entry("MSFT", new String[]{"Microsoft Corporation", "STOCK"}),
            Map.entry("GOOGL", new String[]{"Alphabet Inc. Class A", "STOCK"}),
            Map.entry("TSLA", new String[]{"Tesla, Inc.", "STOCK"}),
            Map.entry("SPY", new String[]{"SPDR S&P 500 ETF Trust", "ETF"}),
            Map.entry("NVDA", new String[]{"NVIDIA Corporation", "STOCK"}),
            Map.entry("AMZN", new String[]{"Amazon.com, Inc.", "STOCK"}),
            Map.entry("META", new String[]{"Meta Platforms, Inc.", "STOCK"}),
            Map.entry("NFLX", new String[]{"Netflix, Inc.", "STOCK"}),
            Map.entry("AMD", new String[]{"Advanced Micro Devices, Inc.", "STOCK"}),
            Map.entry("INTC", new String[]{"Intel Corporation", "STOCK"}),
            Map.entry("JPM", new String[]{"JPMorgan Chase & Co.", "STOCK"}),
            Map.entry("V", new String[]{"Visa Inc.", "STOCK"}),
            Map.entry("MA", new String[]{"Mastercard Incorporated", "STOCK"}),
            Map.entry("JNJ", new String[]{"Johnson & Johnson", "STOCK"}),
            Map.entry("WMT", new String[]{"Walmart Inc.", "STOCK"}),
            Map.entry("PG", new String[]{"Procter & Gamble Co.", "STOCK"}),
            Map.entry("DIS", new String[]{"The Walt Disney Company", "STOCK"}),
            Map.entry("KO", new String[]{"The Coca-Cola Company", "STOCK"}),
            Map.entry("PEP", new String[]{"PepsiCo, Inc.", "STOCK"}),
            Map.entry("XOM", new String[]{"Exxon Mobil Corporation", "STOCK"}),
            Map.entry("BAC", new String[]{"Bank of America Corporation", "STOCK"}),
            Map.entry("ORCL", new String[]{"Oracle Corporation", "STOCK"}),
            Map.entry("CRM", new String[]{"Salesforce, Inc.", "STOCK"}),
            Map.entry("COST", new String[]{"Costco Wholesale Corporation", "STOCK"})
    );

    private static final Set<String> KNOWN_ETFS = Set.of("SPY", "QQQ", "VOO", "VTI", "IVV", "DIA");

    private static final List<String> DEMO_SYMBOLS = List.copyOf(KNOWN_INSTRUMENTS.keySet());

    /** Growth/higher-beta names get more daily volatility in the synthetic walk, for realistic risk metrics. */
    private static final Set<String> HIGH_VOL_SYMBOLS = Set.of("TSLA", "NVDA", "AMD", "META", "NFLX");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    };

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final String seedDir;

    public HistoricalDataLoader(InstrumentRepository instrumentRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 @Value("${app.seed-dir:../infra/db/seed}") String seedDir) {
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.seedDir = seedDir;
    }

    @Override
    public void run(String... args) throws Exception {
        Path dir = Paths.get(seedDir);
        List<Path> csvFiles = listCsvFiles(dir);

        if (!csvFiles.isEmpty()) {
            log.info("Found {} CSV file(s) in {} — loading historical prices", csvFiles.size(), dir.toAbsolutePath());
            for (Path csv : csvFiles) {
                loadCsv(csv);
            }
        } else {
            log.info("No CSVs found in {} — using synthetic data for any demo symbol without history", dir.toAbsolutePath());
        }

        // Per-symbol idempotent: only backfills symbols that still have zero rows (e.g. newly
        // added demo symbols on an existing DB), never touches ones already loaded from CSV/before.
        List<String> missing = DEMO_SYMBOLS.stream()
                .filter(symbol -> priceHistoryRepository.countBySymbol(symbol) == 0)
                .toList();
        if (!missing.isEmpty()) {
            log.warn("SYNTHETIC placeholder data — replace with real Kaggle CSVs in infra/db/seed/ (symbols: {})", missing);
            for (String symbol : missing) {
                generateSynthetic(symbol);
            }
        }
    }

    private List<Path> listCsvFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted()
                    .toList();
        }
    }

    private void loadCsv(Path csv) {
        String symbol = stripExtension(csv.getFileName().toString()).toUpperCase(Locale.ROOT);
        long existing = priceHistoryRepository.countBySymbol(symbol);
        if (existing > 0) {
            log.info("Symbol {} already has {} row(s) in price_history — skipping {}", symbol, existing, csv.getFileName());
            return;
        }

        ensureInstrument(symbol);

        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("CSV {} is empty — skipping", csv.getFileName());
                return;
            }
            Map<String, Integer> idx = indexHeaders(headerLine);

            List<PriceHistory> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                try {
                    LocalDate date = parseDate(col(cols, idx, "date"));
                    BigDecimal open = parseDecimal(col(cols, idx, "open"));
                    BigDecimal high = parseDecimal(col(cols, idx, "high"));
                    BigDecimal low = parseDecimal(col(cols, idx, "low"));
                    BigDecimal close = parseDecimal(col(cols, idx, "close"));
                    BigDecimal adjClose = parseDecimal(col(cols, idx, "adjclose"));
                    Long volume = parseLong(col(cols, idx, "volume"));
                    if (date == null || close == null) continue;
                    rows.add(new PriceHistory(symbol, date, open, high, low, close,
                            adjClose != null ? adjClose : close, volume));
                } catch (Exception rowEx) {
                    log.debug("Skipping malformed row in {}: {}", csv.getFileName(), line);
                }
            }
            priceHistoryRepository.saveAll(rows);
            log.info("Loaded {} row(s) for {} from {}", rows.size(), symbol, csv.getFileName());
        } catch (IOException e) {
            log.warn("Failed to read CSV {}: {}", csv, e.getMessage());
        }
    }

    private Map<String, Integer> indexHeaders(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].replace("\"", "").trim().toLowerCase(Locale.ROOT)
                    .replace(" ", "").replace("_", "");
            idx.put(h, i);
        }
        return idx;
    }

    private String col(String[] cols, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i >= cols.length) return null;
        return cols[i].replace("\"", "").trim();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return (long) Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private void ensureInstrument(String symbol) {
        String[] meta = KNOWN_INSTRUMENTS.get(symbol);
        String name = meta != null ? meta[0] : symbol;
        String assetType = meta != null ? meta[1] : (KNOWN_ETFS.contains(symbol) ? "ETF" : "STOCK");
        instrumentRepository.save(new Instrument(symbol, name, assetType, "USD"));
    }

    private static final Map<String, Double> SYNTHETIC_START_PRICES = Map.ofEntries(
            Map.entry("AAPL", 180.0),
            Map.entry("MSFT", 380.0),
            Map.entry("GOOGL", 140.0),
            Map.entry("TSLA", 250.0),
            Map.entry("SPY", 450.0),
            Map.entry("NVDA", 130.0),
            Map.entry("AMZN", 185.0),
            Map.entry("META", 490.0),
            Map.entry("NFLX", 650.0),
            Map.entry("AMD", 160.0),
            Map.entry("INTC", 32.0),
            Map.entry("JPM", 195.0),
            Map.entry("V", 275.0),
            Map.entry("MA", 460.0),
            Map.entry("JNJ", 155.0),
            Map.entry("WMT", 68.0),
            Map.entry("PG", 165.0),
            Map.entry("DIS", 112.0),
            Map.entry("KO", 62.0),
            Map.entry("PEP", 170.0),
            Map.entry("XOM", 115.0),
            Map.entry("BAC", 38.0),
            Map.entry("ORCL", 125.0),
            Map.entry("CRM", 280.0),
            Map.entry("COST", 730.0)
    );

    private void generateSynthetic(String symbol) {
        ensureInstrument(symbol);

        double startPrice = SYNTHETIC_START_PRICES.getOrDefault(symbol, 100.0);
        double dailyDrift = 0.0004;
        double dailyVol = HIGH_VOL_SYMBOLS.contains(symbol) ? 0.032 : 0.016;

        Random random = new Random(symbol.hashCode());
        LocalDate start = LocalDate.now().minusYears(2);
        LocalDate today = LocalDate.now();

        List<PriceHistory> rows = new ArrayList<>();
        double close = startPrice;
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() >= 6) continue; // skip weekends

            double changePct = dailyDrift + random.nextGaussian() * dailyVol;
            double newClose = Math.max(1.0, close * (1 + changePct));
            double open = close;
            double high = Math.max(open, newClose) * (1 + Math.abs(random.nextGaussian()) * 0.004);
            double low = Math.min(open, newClose) * (1 - Math.abs(random.nextGaussian()) * 0.004);
            long volume = 1_000_000L + (long) (random.nextDouble() * 5_000_000L);

            rows.add(new PriceHistory(symbol, date, bd(open), bd(high), bd(low), bd(newClose), bd(newClose), volume));
            close = newClose;
        }
        priceHistoryRepository.saveAll(rows);
        log.info("Generated {} synthetic daily price row(s) for {}", rows.size(), symbol);
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(Math.round(v * 10000.0) / 10000.0);
    }
}
