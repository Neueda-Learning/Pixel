package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.exception.BadRequestException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    public List<TransactionResponseDto> list(String period, LocalDate from, LocalDate to) {
        List<Transaction> txs;
        if (from != null) {
            Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            txs = transactionRepository.findByExecutedAtBetween(start, end);
        } else {
            LocalDate startDate = PeriodUtil.startDateFor(period, LocalDate.now());
            txs = startDate == null
                    ? transactionRepository.findAll()
                    : transactionRepository.findByExecutedAtAfter(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        return txs.stream()
                .sorted(Comparator.comparing(Transaction::getExecutedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public TransactionResponseDto add(TransactionRequestDto request) {
        return toDto(createFrom(request));
    }

    /** Bulk-imports historical transactions (e.g. from CSV) reusing the same validation/backfill path as add(). */
    public List<TransactionResponseDto> importAll(List<TransactionRequestDto> requests) {
        return requests.stream().map(this::createFrom).map(this::toDto).toList();
    }

    public TransactionResponseDto update(Long id, TransactionRequestDto request) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        String txType = request.getTxType().toUpperCase(Locale.ROOT);
        ensureInstrument(symbol);
        if ("SELL".equals(txType)) {
            requireSufficientHoldings(symbol, request.getQuantity(), id);
        }
        tx.setSymbol(symbol);
        tx.setTxType(txType);
        tx.setQuantity(request.getQuantity());
        tx.setPrice(request.getPrice());
        tx.setFees(request.getFees() != null ? request.getFees() : BigDecimal.ZERO);
        if (request.getExecutedAt() != null) {
            tx.setExecutedAt(request.getExecutedAt());
        }
        tx.setNotes(request.getNotes());
        return toDto(transactionRepository.save(tx));
    }

    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }
        transactionRepository.deleteById(id);
    }

    private Transaction createFrom(TransactionRequestDto request) {
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        String txType = request.getTxType().toUpperCase(Locale.ROOT);
        ensureInstrument(symbol);
        if ("SELL".equals(txType)) {
            requireSufficientHoldings(symbol, request.getQuantity(), null);
        }
        Transaction tx = new Transaction();
        tx.setSymbol(symbol);
        tx.setTxType(txType);
        tx.setQuantity(request.getQuantity());
        tx.setPrice(request.getPrice());
        tx.setFees(request.getFees() != null ? request.getFees() : BigDecimal.ZERO);
        tx.setExecutedAt(request.getExecutedAt() != null ? request.getExecutedAt() : Instant.now());
        tx.setNotes(request.getNotes());
        return transactionRepository.save(tx);
    }

    /** Rejects a SELL that would exceed the net shares currently held for the symbol (BUY total minus SELL total, excluding the transaction being edited). */
    private void requireSufficientHoldings(String symbol, BigDecimal sellQuantity, Long excludeTransactionId) {
        BigDecimal held = transactionRepository.findAll().stream()
                .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                .filter(t -> excludeTransactionId == null || !excludeTransactionId.equals(t.getId()))
                .map(t -> "BUY".equalsIgnoreCase(t.getTxType()) ? t.getQuantity() : t.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sellQuantity.compareTo(held) > 0) {
            throw new BadRequestException(String.format(Locale.ROOT,
                    "Cannot sell %s share(s) of %s — only %s currently held.",
                    sellQuantity.stripTrailingZeros().toPlainString(), symbol, held.stripTrailingZeros().toPlainString()));
        }
    }

    private void ensureInstrument(String symbol) {
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
    }

    private TransactionResponseDto toDto(Transaction t) {
        return new TransactionResponseDto(t.getId(), t.getSymbol(), t.getTxType(), t.getQuantity(),
                t.getPrice(), t.getFees(), t.getExecutedAt(), t.getNotes());
    }
}
