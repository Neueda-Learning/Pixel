package com.pixel.portfolio.loader;

import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import com.pixel.portfolio.service.TwelveDataHistoricalService;
import com.pixel.portfolio.util.AssetTypeClassifier;
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
import java.util.stream.Stream;

/**
 * Loads historical daily prices for the symbols actually held in the portfolio (every distinct
 * symbol that appears in the transaction table). Prefers CSVs dropped in the seed folder for a
 * given symbol; for anything still missing, fetches real daily OHLCV from the Twelve Data API
 * and persists it into price_history so it's only ever fetched once. The symbol list is derived
 * from the DB — there is no hardcoded demo/synthetic data.
 */
@Component
public class HistoricalDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataLoader.class);

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    };

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionRepository transactionRepository;
    private final TwelveDataHistoricalService twelveDataService;
    private final String seedDir;

    public HistoricalDataLoader(InstrumentRepository instrumentRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 TransactionRepository transactionRepository,
                                 TwelveDataHistoricalService twelveDataService,
                                 @Value("${app.seed-dir:../infra/db/seed}") String seedDir) {
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.transactionRepository = transactionRepository;
        this.twelveDataService = twelveDataService;
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
        }

        List<String> portfolioSymbols = transactionRepository.findDistinctSymbols();
        if (portfolioSymbols.isEmpty()) {
            log.info("No transactions yet — nothing to backfill");
            return;
        }

        // Per-symbol idempotent: only backfills symbols that still have zero rows.
        List<String> missing = portfolioSymbols.stream()
                .filter(symbol -> priceHistoryRepository.countBySymbol(symbol) == 0)
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        if (!twelveDataService.hasApiKey()) {
            log.warn("TWELVEDATA_API_KEY is not set — no historical price chart for portfolio symbol(s): {}", missing);
            return;
        }

        log.info("Backfilling live historical prices from Twelve Data for portfolio symbol(s): {}", missing);
        for (int i = 0; i < missing.size(); i++) {
            String symbol = missing.get(i);
            List<PriceHistory> rows = twelveDataService.fetchDailyHistory(symbol);
            if (!rows.isEmpty()) {
                ensureInstrument(symbol);
                priceHistoryRepository.saveAll(rows);
                log.info("Loaded {} row(s) for {} from Twelve Data (live)", rows.size(), symbol);
            } else {
                log.warn("No historical data available from Twelve Data for {}", symbol);
            }
            // Free tier is rate-limited to 8 requests/minute — pace calls to avoid 429s.
            if (i < missing.size() - 1) {
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
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
        if (!instrumentRepository.existsById(symbol)) {
            instrumentRepository.save(new Instrument(symbol, symbol, AssetTypeClassifier.classify(symbol, null), "USD"));
        }
    }
}
