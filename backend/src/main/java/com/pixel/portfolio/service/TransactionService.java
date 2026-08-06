package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.LotDto;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if ("SELL".equals(txType)) {
            SellPricing pricing = resolveSellPricing(symbol, request, id);
            tx.setBuyPrice(pricing.buyPrice());
            tx.setBuyTransactionId(pricing.buyTransactionId());
        } else {
            tx.setBuyPrice(null);
            tx.setBuyTransactionId(null);
        }
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
        if ("SELL".equals(txType)) {
            SellPricing pricing = resolveSellPricing(symbol, request, null);
            tx.setBuyPrice(pricing.buyPrice());
            tx.setBuyTransactionId(pricing.buyTransactionId());
        } else {
            tx.setBuyPrice(null);
            tx.setBuyTransactionId(null);
        }
        tx.setFees(request.getFees() != null ? request.getFees() : BigDecimal.ZERO);
        tx.setExecutedAt(request.getExecutedAt() != null ? request.getExecutedAt() : Instant.now());
        tx.setNotes(request.getNotes());
        return transactionRepository.save(tx);
    }

    private record SellPricing(BigDecimal buyPrice, Long buyTransactionId) {}

    /**
     * Resolves the buy price for a SELL, preferring an explicitly selected open lot (buyTransactionId) —
     * validated to belong to the same symbol and to have enough remaining quantity — over a manually
     * supplied buyPrice (the CSV-import path, which has no lot picker).
     */
    private SellPricing resolveSellPricing(String symbol, TransactionRequestDto request, Long excludeTransactionId) {
        if (request.getBuyTransactionId() != null) {
            Transaction buyTx = transactionRepository.findById(request.getBuyTransactionId())
                    .orElseThrow(() -> new BadRequestException("Selected buy lot not found"));
            if (!buyTx.getSymbol().equalsIgnoreCase(symbol) || !"BUY".equalsIgnoreCase(buyTx.getTxType())) {
                throw new BadRequestException("Selected buy lot does not match this symbol");
            }
            BigDecimal remaining = getOpenLots(symbol, excludeTransactionId).stream()
                    .filter(l -> l.getTransactionId().equals(buyTx.getId()))
                    .map(LotDto::getRemainingQuantity)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            if (request.getQuantity().compareTo(remaining) > 0) {
                throw new BadRequestException(String.format(Locale.ROOT,
                        "Cannot sell %s share(s) from this lot \u2014 only %s remaining.",
                        request.getQuantity().stripTrailingZeros().toPlainString(),
                        remaining.stripTrailingZeros().toPlainString()));
            }
            return new SellPricing(buyTx.getPrice(), buyTx.getId());
        }
        // Fallback (e.g. CSV import, no lot picker): require a manually supplied buy price.
        if (request.getBuyPrice() == null) {
            throw new BadRequestException("buyPrice is required for SELL transactions");
        }
        return new SellPricing(request.getBuyPrice(), null);
    }

    /**
     * Open (not-yet-fully-sold) BUY lots for a symbol, oldest first, each with its remaining quantity.
     * SELLs that reference a specific lot (buyTransactionId) draw from it first; any unassigned SELL
     * quantity (e.g. CSV imports with only a buyPrice) is drawn FIFO from the oldest remaining lots.
     */
    public List<LotDto> getOpenLots(String symbolParam, Long excludeTransactionId) {
        String symbol = symbolParam.trim().toUpperCase(Locale.ROOT);
        List<Transaction> txs = transactionRepository.findAll().stream()
                .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                .filter(t -> excludeTransactionId == null || !excludeTransactionId.equals(t.getId()))
                .sorted(Comparator.comparing(Transaction::getExecutedAt).thenComparing(Transaction::getId))
                .toList();

        Map<Long, BigDecimal> remainingByLot = new LinkedHashMap<>();
        for (Transaction t : txs) {
            if ("BUY".equalsIgnoreCase(t.getTxType())) {
                remainingByLot.put(t.getId(), t.getQuantity());
            }
        }
        for (Transaction t : txs) {
            if (!"SELL".equalsIgnoreCase(t.getTxType())) continue;
            BigDecimal toConsume = t.getQuantity();
            if (t.getBuyTransactionId() != null && remainingByLot.containsKey(t.getBuyTransactionId())) {
                BigDecimal avail = remainingByLot.get(t.getBuyTransactionId());
                BigDecimal used = toConsume.min(avail);
                remainingByLot.put(t.getBuyTransactionId(), avail.subtract(used));
                toConsume = toConsume.subtract(used);
            }
            for (Long lotId : remainingByLot.keySet()) {
                if (toConsume.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal avail = remainingByLot.get(lotId);
                if (avail.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal used = toConsume.min(avail);
                remainingByLot.put(lotId, avail.subtract(used));
                toConsume = toConsume.subtract(used);
            }
        }

        List<LotDto> lots = new ArrayList<>();
        for (Transaction t : txs) {
            if (!"BUY".equalsIgnoreCase(t.getTxType())) continue;
            BigDecimal remaining = remainingByLot.getOrDefault(t.getId(), BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                lots.add(new LotDto(t.getId(), t.getPrice(), t.getExecutedAt(), remaining));
            }
        }
        return lots;
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
                t.getPrice(), t.getBuyPrice(), t.getBuyTransactionId(), t.getFees(), t.getExecutedAt(), t.getNotes());
    }
}
