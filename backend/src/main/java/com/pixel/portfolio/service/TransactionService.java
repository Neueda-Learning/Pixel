package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import com.pixel.portfolio.util.PeriodUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TwelveDataHistoricalService twelveDataService;

    public TransactionService(TransactionRepository transactionRepository,
                               InstrumentRepository instrumentRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               TwelveDataHistoricalService twelveDataService) {
        this.transactionRepository = transactionRepository;
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.twelveDataService = twelveDataService;
    }

    public List<TransactionResponseDto> list(String period) {
        LocalDate startDate = PeriodUtil.startDateFor(period, LocalDate.now());
        List<Transaction> txs = startDate == null
                ? transactionRepository.findAll()
                : transactionRepository.findByExecutedAtAfter(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        return txs.stream()
                .sorted(Comparator.comparing(Transaction::getExecutedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public TransactionResponseDto add(TransactionRequestDto request) {
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (!instrumentRepository.existsById(symbol)) {
            instrumentRepository.save(new Instrument(symbol, symbol, "STOCK", "USD"));
        }
        // New symbol added to the portfolio: fetch its real historical chart data from
        // Twelve Data right away (instead of waiting for the next app restart) so the
        // instrument detail page has a chart immediately.
        if (priceHistoryRepository.countBySymbol(symbol) == 0 && twelveDataService.hasApiKey()) {
            try {
                List<PriceHistory> rows = twelveDataService.fetchDailyHistory(symbol);
                if (!rows.isEmpty()) {
                    priceHistoryRepository.saveAll(rows);
                    log.info("Loaded {} historical row(s) for new portfolio symbol {} from Twelve Data", rows.size(), symbol);
                }
            } catch (Exception e) {
                log.warn("Failed to backfill historical data for {}: {}", symbol, e.getMessage());
            }
        }
        Transaction tx = new Transaction();
        tx.setSymbol(request.getSymbol().trim().toUpperCase(Locale.ROOT));
        tx.setTxType(request.getTxType().toUpperCase(Locale.ROOT));
        tx.setQuantity(request.getQuantity());
        tx.setPrice(request.getPrice());
        tx.setFees(request.getFees() != null ? request.getFees() : BigDecimal.ZERO);
        tx.setExecutedAt(request.getExecutedAt() != null ? request.getExecutedAt() : Instant.now());
        tx.setNotes(request.getNotes());
        return toDto(transactionRepository.save(tx));
    }

    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }
        transactionRepository.deleteById(id);
    }

    public List<TransactionResponseDto> importCsv(MultipartFile file) {
        List<TransactionResponseDto> imported = new ArrayList<>();
        try {
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim().replace("\r", "");
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("##")) continue;
                // Skip any header row (first token is not a ticker-like string or is a known header word)
                String firstToken = line.split(",")[0].trim().replace("\"", "").toLowerCase();
                if (firstToken.equals("symbol") || firstToken.equals("id") || firstToken.equals("ticker")) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 3) continue;
                try {
                    String sym = cols[0].trim().replace("\"", "").toUpperCase();
                    if (sym.isEmpty()) continue;

                    TransactionRequestDto req = new TransactionRequestDto();
                    req.setSymbol(sym);

                    // Auto-detect format:
                    // Format A (with txType): symbol, BUY|SELL, quantity, price, [fees,] date, [notes]
                    // Format B (without txType): symbol, quantity, price, date, [notes]
                    String col1 = cols[1].trim().replace("\"", "").toUpperCase();
                    boolean hasTxType = col1.equals("BUY") || col1.equals("SELL");

                    if (hasTxType) {
                        // Format A
                        req.setTxType(col1);
                        req.setQuantity(new BigDecimal(cols[2].trim().replace("\"", "")));
                        req.setPrice(cols.length > 3 && !cols[3].trim().isEmpty()
                                ? new BigDecimal(cols[3].trim().replace("\"", "")) : BigDecimal.ONE);
                        req.setFees(BigDecimal.ZERO);
                        String dateStr = cols.length > 5 && !cols[5].trim().isEmpty() ? cols[5].trim().replace("\"", "")
                                       : cols.length > 4 && !cols[4].trim().isEmpty() ? cols[4].trim().replace("\"", "") : "";
                        req.setExecutedAt(parseDate(dateStr));
                    } else {
                        // Format B: symbol, quantity, price, date
                        req.setTxType("BUY");
                        req.setQuantity(new BigDecimal(col1));
                        req.setPrice(cols.length > 2 && !cols[2].trim().isEmpty()
                                ? new BigDecimal(cols[2].trim().replace("\"", "")) : BigDecimal.ONE);
                        req.setFees(BigDecimal.ZERO);
                        String dateStr = cols.length > 3 && !cols[3].trim().isEmpty() ? cols[3].trim().replace("\"", "") : "";
                        req.setExecutedAt(parseDate(dateStr));
                    }

                    imported.add(add(req));
                } catch (Exception ignored) { /* skip malformed rows */ }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage());
        }
        return imported;
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return Instant.now();
        try {
            // Try ISO instant: 2024-01-15T00:00:00Z
            return Instant.parse(dateStr);
        } catch (Exception ignored) {}
        try {
            // Try date-only: 2024-01-15
            return java.time.LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception ignored) {}
        return Instant.now();
    }

    private TransactionResponseDto toDto(Transaction t) {
        return new TransactionResponseDto(t.getId(), t.getSymbol(), t.getTxType(), t.getQuantity(),
                t.getPrice(), t.getFees(), t.getExecutedAt(), t.getNotes());
    }
}
