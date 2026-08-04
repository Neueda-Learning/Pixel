package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.dto.QuoteDto;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock InstrumentRepository instrumentRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock MarketDataService marketDataService;

    @InjectMocks PortfolioService portfolioService;

    private static Transaction tx(String symbol, String type, double qty, double price, double fees, Instant when) {
        Transaction t = new Transaction();
        t.setSymbol(symbol);
        t.setTxType(type);
        t.setQuantity(BigDecimal.valueOf(qty));
        t.setPrice(BigDecimal.valueOf(price));
        t.setFees(BigDecimal.valueOf(fees));
        t.setExecutedAt(when);
        return t;
    }

    @Test
    void getHoldings_averagesCostAcrossMultipleBuys() {
        Instant day1 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(30, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("AAPL", "BUY", 10, 150.00, 1.5, day1),
                tx("AAPL", "BUY", 5, 170.00, 1.5, day2)
        ));
        when(marketDataService.getQuote("AAPL"))
                .thenReturn(new QuoteDto("AAPL", BigDecimal.valueOf(200), null, null, null, null, null, null, "LIVE", Instant.now()));

        List<HoldingDto> holdings = portfolioService.getHoldings();

        assertEquals(1, holdings.size());
        HoldingDto aapl = holdings.get(0);
        assertEquals(0, BigDecimal.valueOf(15).compareTo(aapl.getQuantity()));
        // (10*150 + 1.5 + 5*170 + 1.5) / 15 = 156.8667
        assertEquals(0, new BigDecimal("156.8667").compareTo(aapl.getAvgCost()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(aapl.getMarketValue())); // 15 * 200
        assertEquals("LIVE", aapl.getPriceSource());
    }

    @Test
    void getHoldings_sellReducesQuantityButKeepsWeightedAvgCost() {
        Instant day1 = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant day3 = Instant.now().minus(30, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("AAPL", "BUY", 10, 150.00, 1.5, day1),
                tx("AAPL", "BUY", 5, 170.00, 1.5, day2),
                tx("AAPL", "SELL", 5, 200.00, 0, day3)
        ));
        when(marketDataService.getQuote("AAPL"))
                .thenReturn(new QuoteDto("AAPL", BigDecimal.valueOf(200), null, null, null, null, null, null, "LIVE", Instant.now()));

        List<HoldingDto> holdings = portfolioService.getHoldings();

        assertEquals(1, holdings.size());
        HoldingDto aapl = holdings.get(0);
        assertEquals(0, BigDecimal.valueOf(10).compareTo(aapl.getQuantity()));
        // average cost is unaffected by a sell under the weighted-average-cost method
        assertEquals(0, new BigDecimal("156.8667").compareTo(aapl.getAvgCost()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(aapl.getMarketValue())); // 10 * 200
        assertTrue(aapl.getGainLoss().doubleValue() > 0);
    }

    @Test
    void getHoldings_fullySoldPosition_isExcluded() {
        Instant day1 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(30, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("AAPL", "BUY", 10, 150.00, 0, day1),
                tx("AAPL", "SELL", 10, 180.00, 0, day2)
        ));

        List<HoldingDto> holdings = portfolioService.getHoldings();

        assertEquals(0, holdings.size());
    }

    @Test
    void getSummary_aggregatesTotalsAndAllocationByAssetType() {
        Instant day1 = Instant.now().minus(60, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("AAPL", "BUY", 10, 100.00, 0, day1),
                tx("SPY", "BUY", 2, 400.00, 0, day1)
        ));
        when(marketDataService.getQuote("AAPL"))
                .thenReturn(new QuoteDto("AAPL", BigDecimal.valueOf(120), null, null, null, null, null, null, "LIVE", Instant.now()));
        when(marketDataService.getQuote("SPY"))
                .thenReturn(new QuoteDto("SPY", BigDecimal.valueOf(420), null, null, null, null, null, null, "LIVE", Instant.now()));

        PortfolioSummaryDto summary = portfolioService.getSummary();

        // totalValue = 10*120 + 2*420 = 1200 + 840 = 2040; totalCost = 1000 + 800 = 1800
        assertEquals(0, new BigDecimal("2040.00").compareTo(summary.getTotalValue()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(summary.getTotalCost()));
        assertEquals(0, new BigDecimal("240.00").compareTo(summary.getTotalGainLoss()));
        assertEquals(2, summary.getHoldingsCount());
    }

    @Test
    void getHoldings_marketDataFailure_fallsBackToDbPriceInsteadOfThrowing() {
        Instant day1 = Instant.now().minus(60, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("AAPL", "BUY", 10, 150.00, 0, day1)
        ));
        when(marketDataService.getQuote(anyString())).thenThrow(new RuntimeException("finnhub down"));

        List<HoldingDto> holdings = portfolioService.getHoldings();

        assertEquals(1, holdings.size());
        assertEquals("UNAVAILABLE", holdings.get(0).getPriceSource());
    }
}
