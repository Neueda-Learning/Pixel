package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.AllocationDto;
import com.pixel.portfolio.dto.ChatResponseDto;
import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PerformancePointDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.dto.RiskDto;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.repository.InstrumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatBotServiceTest {

    @Mock PortfolioService portfolioService;
    @Mock RiskService riskService;
    @Mock InstrumentRepository instrumentRepository;

    ChatBotService chatBotService;

    @BeforeEach
    void setUp() {
        chatBotService = new ChatBotService(portfolioService, riskService, instrumentRepository);
        lenient().when(instrumentRepository.findAll()).thenReturn(List.of(
                new Instrument("AAPL", "Apple Inc.", "STOCK", "USD"),
                new Instrument("MSFT", "Microsoft Corp.", "STOCK", "USD")));
    }

    private static HoldingDto holding(String symbol, String name, BigDecimal marketValue, BigDecimal gainLoss, BigDecimal gainLossPct) {
        return new HoldingDto(symbol, name, "STOCK", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                marketValue, gainLoss, gainLossPct, "LIVE");
    }

    @Test
    void greeting_returnsHelpMessage() {
        ChatResponseDto response = chatBotService.respond("hello");
        assertTrue(response.getReply().contains("rule-based portfolio assistant"));
    }

    @Test
    void unrecognizedMessage_returnsFallbackWithHelp() {
        ChatResponseDto response = chatBotService.respond("what's the weather today");
        assertTrue(response.getReply().contains("didn't quite catch that"));
    }

    @Test
    void portfolioValueQuestion_returnsSummary() {
        when(portfolioService.getSummary()).thenReturn(new PortfolioSummaryDto(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(900), BigDecimal.valueOf(100),
                BigDecimal.valueOf(11.11), 2, List.of()));

        ChatResponseDto response = chatBotService.respond("What's my portfolio worth?");

        assertTrue(response.getReply().contains("$1000"));
        assertTrue(response.getReply().contains("2 holding"));
    }

    @Test
    void bestPerformerQuestion_picksHighestGainLossPct() {
        when(portfolioService.getHoldings()).thenReturn(List.of(
                holding("AAPL", "Apple Inc.", BigDecimal.valueOf(1000), BigDecimal.valueOf(50), BigDecimal.valueOf(5)),
                holding("MSFT", "Microsoft Corp.", BigDecimal.valueOf(2000), BigDecimal.valueOf(400), BigDecimal.valueOf(20))));

        ChatResponseDto response = chatBotService.respond("What's my best performer?");

        assertTrue(response.getReply().contains("MSFT"));
    }

    @Test
    void worstPerformerQuestion_picksLowestGainLossPct() {
        when(portfolioService.getHoldings()).thenReturn(List.of(
                holding("AAPL", "Apple Inc.", BigDecimal.valueOf(1000), BigDecimal.valueOf(-50), BigDecimal.valueOf(-5)),
                holding("MSFT", "Microsoft Corp.", BigDecimal.valueOf(2000), BigDecimal.valueOf(400), BigDecimal.valueOf(20))));

        ChatResponseDto response = chatBotService.respond("What's my worst holding?");

        assertTrue(response.getReply().contains("AAPL"));
    }

    @Test
    void allocationQuestion_suggestsRebalance_whenAssetTypeExceeds40Percent() {
        when(portfolioService.getSummary()).thenReturn(new PortfolioSummaryDto(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(900), BigDecimal.valueOf(100), BigDecimal.valueOf(11.11), 2,
                List.of(new AllocationDto("STOCK", BigDecimal.valueOf(500), BigDecimal.valueOf(50)))));

        ChatResponseDto response = chatBotService.respond("Should I rebalance?");

        assertTrue(response.getReply().contains("rebalancing"));
        assertTrue(response.getReply().contains("STOCK"));
    }

    @Test
    void allocationQuestion_noSuggestion_whenBalanced() {
        when(portfolioService.getSummary()).thenReturn(new PortfolioSummaryDto(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(900), BigDecimal.valueOf(100), BigDecimal.valueOf(11.11), 3,
                List.of(new AllocationDto("STOCK", BigDecimal.valueOf(350), BigDecimal.valueOf(35)),
                        new AllocationDto("ETF", BigDecimal.valueOf(350), BigDecimal.valueOf(35)),
                        new AllocationDto("COMMODITY", BigDecimal.valueOf(300), BigDecimal.valueOf(30)))));

        ChatResponseDto response = chatBotService.respond("What's my allocation?");

        assertTrue(response.getReply().contains("diversification looks healthy"));
    }

    @Test
    void riskQuestionWithSymbol_returnsRecommendation() {
        RiskDto risk = new RiskDto("AAPL", LocalDate.now(), 100, 0.2, 0.1, 1.2, -0.1, 1.0, 100.0, 90.0,
                "BULLISH", 55.0, "BUY", "Sharpe ratio looks attractive.");
        when(riskService.getRisk("AAPL")).thenReturn(risk);

        ChatResponseDto response = chatBotService.respond("Should I buy AAPL?");

        assertTrue(response.getReply().contains("AAPL"));
        assertTrue(response.getReply().contains("BUY"));
    }

    @Test
    void riskQuestionWithoutSymbol_asksForClarification() {
        ChatResponseDto response = chatBotService.respond("What's the risk?");
        assertTrue(response.getReply().contains("Which symbol"));
    }

    @Test
    void riskQuestion_symbolWithNoPriceHistory_returnsServiceMessage() {
        when(riskService.getRisk("AAPL")).thenThrow(new ResourceNotFoundException("No price history available for AAPL"));

        ChatResponseDto response = chatBotService.respond("What's the risk on AAPL?");

        assertTrue(response.getReply().contains("No price history available for AAPL"));
    }

    @Test
    void holdingsQuestion_listsAllHoldings() {
        when(portfolioService.getHoldings()).thenReturn(List.of(
                holding("AAPL", "Apple Inc.", BigDecimal.valueOf(1000), BigDecimal.valueOf(50), BigDecimal.valueOf(5))));

        ChatResponseDto response = chatBotService.respond("What are my holdings?");

        assertTrue(response.getReply().contains("AAPL"));
    }

    @Test
    void performanceQuestion_computesChangeOverPeriod() {
        when(portfolioService.getPerformance("3M")).thenReturn(List.of(
                new PerformancePointDto(LocalDate.now().minusMonths(3), BigDecimal.valueOf(900)),
                new PerformancePointDto(LocalDate.now(), BigDecimal.valueOf(1000))));

        ChatResponseDto response = chatBotService.respond("What's my performance over 3M?");

        assertTrue(response.getReply().contains("$900"));
        assertTrue(response.getReply().contains("$1000"));
    }
}
