package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.AllocationDto;
import com.pixel.portfolio.dto.ChatResponseDto;
import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PerformancePointDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.dto.RiskDto;
import com.pixel.portfolio.exception.BadRequestException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.repository.InstrumentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, rule-based portfolio assistant — no LLM. Matches the user's message
 * against a fixed set of keyword/threshold heuristics and answers using real data from
 * PortfolioService/RiskService. Order of checks matters: more specific intents (risk
 * for a named symbol) are checked before generic ones (holdings list).
 */
@Service
public class ChatBotService {

    private static final BigDecimal REBALANCE_THRESHOLD_PCT = BigDecimal.valueOf(40);
    private static final Pattern PERIOD_PATTERN = Pattern.compile("\\b(1M|3M|6M|1Y|ALL)\\b", Pattern.CASE_INSENSITIVE);

    private final PortfolioService portfolioService;
    private final RiskService riskService;
    private final InstrumentRepository instrumentRepository;

    public ChatBotService(PortfolioService portfolioService, RiskService riskService,
                           InstrumentRepository instrumentRepository) {
        this.portfolioService = portfolioService;
        this.riskService = riskService;
        this.instrumentRepository = instrumentRepository;
    }

    public ChatResponseDto respond(String rawMessage) {
        String message = rawMessage.toLowerCase(Locale.ROOT).trim();
        Optional<String> symbol = extractSymbol(rawMessage);

        String reply;
        if (isGreeting(message)) {
            reply = helpMessage();
        } else if ((message.contains("risk") || message.contains("recommend") || message.contains("should i buy")
                || message.contains("should i sell") || message.contains("buy or sell")) && symbol.isPresent()) {
            reply = riskReply(symbol.get());
        } else if (message.contains("risk") || message.contains("recommend")) {
            reply = "Which symbol do you want a risk check on? " + knownSymbolsHint();
        } else if (message.contains("best performer") || message.contains("top performer") || message.contains("top holding")) {
            reply = bestOrWorstPerformerReply(true);
        } else if (message.contains("worst performer") || message.contains("worst holding")) {
            reply = bestOrWorstPerformerReply(false);
        } else if (message.contains("rebalance") || message.contains("allocation") || message.contains("diversif")) {
            reply = allocationReply();
        } else if (message.contains("how many") && (message.contains("holding") || message.contains("stock") || message.contains("position"))) {
            reply = holdingsCountReply();
        } else if (message.contains("holding") || message.contains("what do i own") || message.contains("position") || message.contains("list")) {
            reply = holdingsListReply();
        } else if (message.contains("performance") || message.contains("gain") || message.contains("loss") || message.contains("profit")) {
            reply = performanceReply(message);
        } else if (message.contains("value") || message.contains("worth") || message.contains("summary")) {
            reply = summaryReply();
        } else {
            reply = fallbackReply();
        }

        return new ChatResponseDto(reply);
    }

    private boolean isGreeting(String message) {
        return message.isEmpty() || message.equals("hi") || message.equals("hello") || message.equals("hey")
                || message.contains("help") || message.contains("what can you");
    }

    private String helpMessage() {
        return "I'm a rule-based portfolio assistant — I can answer questions like:\n"
                + "- \"What's my portfolio worth?\"\n"
                + "- \"What's my best/worst performer?\"\n"
                + "- \"What are my holdings?\"\n"
                + "- \"What's my performance this month?\" (1M/3M/6M/1Y/ALL)\n"
                + "- \"Should I rebalance?\" / \"What's my allocation?\"\n"
                + "- \"What's the risk on AAPL?\" / \"Should I buy AAPL?\"";
    }

    private String riskReply(String symbol) {
        RiskDto risk;
        try {
            risk = riskService.getRisk(symbol);
        } catch (ResourceNotFoundException | BadRequestException e) {
            return e.getMessage();
        }
        return String.format(Locale.ROOT,
                "%s: %s. %s (Sharpe %.2f, volatility %.1f%%, max drawdown %.1f%%, trend %s)",
                risk.getSymbol(), risk.getRecommendation(), risk.getRationale(),
                risk.getSharpeRatio(), risk.getAnnualizedVolatility() * 100, risk.getMaxDrawdown() * 100, risk.getTrend());
    }

    private String bestOrWorstPerformerReply(boolean best) {
        List<HoldingDto> holdings = portfolioService.getHoldings();
        if (holdings.isEmpty()) return "You don't have any holdings yet.";

        Comparator<HoldingDto> byGainLossPct = Comparator.comparing(HoldingDto::getGainLossPct);
        HoldingDto pick = best
                ? holdings.stream().max(byGainLossPct).orElseThrow()
                : holdings.stream().min(byGainLossPct).orElseThrow();

        return String.format(Locale.ROOT, "Your %s performer is %s (%s), %s%.2f%% (%s$%s).",
                best ? "best" : "worst", pick.getSymbol(), pick.getName(),
                pick.getGainLossPct().signum() >= 0 ? "+" : "", pick.getGainLossPct(),
                pick.getGainLoss().signum() >= 0 ? "+" : "", pick.getGainLoss());
    }

    private String allocationReply() {
        PortfolioSummaryDto summary = portfolioService.getSummary();
        List<AllocationDto> allocation = summary.getAllocation();
        if (allocation.isEmpty()) return "You don't have any holdings yet, so there's nothing to allocate.";

        StringBuilder sb = new StringBuilder("Current allocation by asset type:\n");
        List<String> overweight = new ArrayList<>();
        for (AllocationDto a : allocation) {
            sb.append(String.format(Locale.ROOT, "- %s: %.1f%% ($%s)\n", a.getAssetType(), a.getPercentage(), a.getValue()));
            if (a.getPercentage().compareTo(REBALANCE_THRESHOLD_PCT) > 0) {
                overweight.add(a.getAssetType());
            }
        }
        if (overweight.isEmpty()) {
            sb.append("No single asset type exceeds 40% of your portfolio — diversification looks healthy.");
        } else {
            sb.append("Suggestion: ").append(String.join(", ", overweight))
                    .append(overweight.size() > 1 ? " each make up" : " makes up")
                    .append(" over 40% of your portfolio — consider rebalancing to reduce concentration risk.");
        }
        return sb.toString();
    }

    private String holdingsCountReply() {
        PortfolioSummaryDto summary = portfolioService.getSummary();
        return "You currently hold " + summary.getHoldingsCount() + " position(s), worth $" + summary.getTotalValue() + " in total.";
    }

    private String holdingsListReply() {
        List<HoldingDto> holdings = portfolioService.getHoldings();
        if (holdings.isEmpty()) return "You don't have any holdings yet.";

        StringBuilder sb = new StringBuilder("Your holdings:\n");
        holdings.stream()
                .sorted(Comparator.comparing(HoldingDto::getMarketValue).reversed())
                .forEach(h -> sb.append(String.format(Locale.ROOT, "- %s: %s shares, $%s (%s%.2f%%)\n",
                        h.getSymbol(), h.getQuantity(), h.getMarketValue(),
                        h.getGainLossPct().signum() >= 0 ? "+" : "", h.getGainLossPct())));
        return sb.toString().stripTrailing();
    }

    private String performanceReply(String message) {
        String period = extractPeriod(message).orElse("1M");
        List<PerformancePointDto> points = portfolioService.getPerformance(period);
        if (points.size() < 2) {
            return "Not enough price history to compute performance over " + period + ".";
        }
        BigDecimal start = points.get(0).getValue();
        BigDecimal end = points.get(points.size() - 1).getValue();
        BigDecimal change = end.subtract(start);
        BigDecimal changePct = start.compareTo(BigDecimal.ZERO) > 0
                ? change.divide(start, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        return String.format(Locale.ROOT, "Over %s, your portfolio went from $%s to $%s (%s$%s, %s%.2f%%).",
                period, start, end, change.signum() >= 0 ? "+" : "", change,
                changePct.signum() >= 0 ? "+" : "", changePct);
    }

    private String summaryReply() {
        PortfolioSummaryDto summary = portfolioService.getSummary();
        if (summary.getHoldingsCount() == 0) return "You don't have any holdings yet.";
        return String.format(Locale.ROOT,
                "Your portfolio is worth $%s across %d holding(s), with a total gain/loss of %s$%s (%s%.2f%%).",
                summary.getTotalValue(), summary.getHoldingsCount(),
                summary.getTotalGainLoss().signum() >= 0 ? "+" : "", summary.getTotalGainLoss(),
                summary.getTotalGainLossPct().signum() >= 0 ? "+" : "", summary.getTotalGainLossPct());
    }

    private String fallbackReply() {
        return "I didn't quite catch that. " + helpMessage();
    }

    private Optional<String> extractPeriod(String message) {
        Matcher matcher = PERIOD_PATTERN.matcher(message);
        return matcher.find() ? Optional.of(matcher.group(1).toUpperCase(Locale.ROOT)) : Optional.empty();
    }

    private Optional<String> extractSymbol(String rawMessage) {
        Set<String> knownSymbols = instrumentRepository.findAll().stream()
                .map(Instrument::getSymbol)
                .collect(Collectors.toSet());
        for (String token : rawMessage.split("[^A-Za-z]+")) {
            String candidate = token.toUpperCase(Locale.ROOT);
            if (knownSymbols.contains(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String knownSymbolsHint() {
        List<String> symbols = instrumentRepository.findAll().stream()
                .map(Instrument::getSymbol)
                .sorted()
                .limit(10)
                .toList();
        return symbols.isEmpty() ? "" : "Try one of: " + String.join(", ", symbols) + ".";
    }
}
