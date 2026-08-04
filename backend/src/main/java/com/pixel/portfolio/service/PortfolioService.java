package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives holdings and portfolio value entirely from the transaction ledger —
 * there is no separate holdings table, per design.
 */
@Service
public class PortfolioService {

    private static final int SCALE = 6;

    private final TransactionRepository transactionRepository;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MarketDataService marketDataService;

    public PortfolioService(TransactionRepository transactionRepository,
                             InstrumentRepository instrumentRepository,
                             PriceHistoryRepository priceHistoryRepository,
                             MarketDataService marketDataService) {
        this.transactionRepository = transactionRepository;
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketDataService = marketDataService;
    }

    public List<HoldingDto> getHoldings() {
        Map<String, List<Transaction>> bySymbol = groupBySymbol(transactionRepository.findAll());

        List<HoldingDto> holdings = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : bySymbol.entrySet()) {
            String symbol = entry.getKey();
            Position position = computePosition(entry.getValue());
            if (position.qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            Instrument instrument = instrumentRepository.findById(symbol).orElse(null);
            String name = instrument != null ? instrument.getName() : symbol;
            String assetType = instrument != null ? instrument.getAssetType() : "STOCK";

            CurrentPrice priced = currentPriceFor(symbol);
            BigDecimal marketValue = priced.price().multiply(position.qty);
            BigDecimal costBasis = position.avgCost.multiply(position.qty);
            BigDecimal gainLoss = marketValue.subtract(costBasis);
            BigDecimal gainLossPct = costBasis.compareTo(BigDecimal.ZERO) > 0
                    ? gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            holdings.add(new HoldingDto(
                    symbol, name, assetType,
                    position.qty.setScale(4, RoundingMode.HALF_UP),
                    position.avgCost.setScale(4, RoundingMode.HALF_UP),
                    priced.price().setScale(4, RoundingMode.HALF_UP),
                    marketValue.setScale(2, RoundingMode.HALF_UP),
                    gainLoss.setScale(2, RoundingMode.HALF_UP),
                    gainLossPct.setScale(2, RoundingMode.HALF_UP),
                    priced.source()));
        }
        holdings.sort(Comparator.comparing(HoldingDto::getSymbol));
        return holdings;
    }

    /** Live quote via MarketDataService (which itself falls back to price_history); degrades to zero if truly unavailable. */
    private CurrentPrice currentPriceFor(String symbol) {
        try {
            var quote = marketDataService.getQuote(symbol);
            return new CurrentPrice(quote.getCurrent(), quote.getSource());
        } catch (Exception e) {
            BigDecimal fallback = priceHistoryRepository.findTopBySymbolOrderByTradeDateDesc(symbol)
                    .map(PriceHistory::getClose)
                    .orElse(BigDecimal.ZERO);
            return new CurrentPrice(fallback, "UNAVAILABLE");
        }
    }

    private record CurrentPrice(BigDecimal price, String source) {}

    private Map<String, List<Transaction>> groupBySymbol(List<Transaction> txs) {
        Map<String, List<Transaction>> map = new HashMap<>();
        for (Transaction tx : txs) {
            map.computeIfAbsent(tx.getSymbol(), k -> new ArrayList<>()).add(tx);
        }
        map.values().forEach(list -> list.sort(Comparator.comparing(Transaction::getExecutedAt)));
        return map;
    }

    private Position computePosition(List<Transaction> txsAsc) {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal costBasis = BigDecimal.ZERO;
        for (Transaction tx : txsAsc) {
            BigDecimal txQty = tx.getQuantity();
            if ("BUY".equalsIgnoreCase(tx.getTxType())) {
                BigDecimal fees = tx.getFees() != null ? tx.getFees() : BigDecimal.ZERO;
                costBasis = costBasis.add(tx.getPrice().multiply(txQty)).add(fees);
                qty = qty.add(txQty);
            } else if ("SELL".equalsIgnoreCase(tx.getTxType()) && qty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgCost = costBasis.divide(qty, SCALE, RoundingMode.HALF_UP);
                BigDecimal sellQty = txQty.min(qty);
                costBasis = costBasis.subtract(avgCost.multiply(sellQty));
                qty = qty.subtract(sellQty);
            }
        }
        BigDecimal avgCost = qty.compareTo(BigDecimal.ZERO) > 0
                ? costBasis.divide(qty, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new Position(qty, avgCost);
    }

    private record Position(BigDecimal qty, BigDecimal avgCost) {}
}

