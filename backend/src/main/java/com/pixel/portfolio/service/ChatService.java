package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.QuoteDto;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Rule-based financial portfolio assistant with real-time market data access.
 * Understands common natural language queries about holdings, transactions,
 * quotes, risk, and provides investment suggestions.
 */
@Service
public class ChatService {

    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final TransactionRepository transactionRepository;

    public ChatService(PortfolioService portfolioService,
                       MarketDataService marketDataService,
                       TransactionRepository transactionRepository) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
        this.transactionRepository = transactionRepository;
    }

    public String respond(String message) {
        if (message == null || message.isBlank()) return "Hello! I'm your Pixel portfolio assistant. Ask me about your holdings, portfolio value, stock prices, or investment insights.";
        String lower = message.toLowerCase(Locale.ROOT).trim();

        // Greetings
        if (matchesAny(lower, "hi", "hello", "hey", "good morning", "good evening", "help")) {
            return greeting();
        }

        // Portfolio summary / value
        if (matchesAny(lower, "portfolio", "total value", "my portfolio", "how much", "net worth", "summary")) {
            return portfolioSummary();
        }

        // Holdings overview
        if (matchesAny(lower, "holding", "what do i own", "my stocks", "my investments", "what i have", "assets")) {
            return holdingsSummary();
        }

        // Best performer
        if (matchesAny(lower, "best perform", "top gainer", "biggest gain", "best stock", "most profit", "which stock is up")) {
            return bestPerformer();
        }

        // Worst performer
        if (matchesAny(lower, "worst perform", "biggest loss", "losing", "worst stock", "which stock is down", "underperform")) {
            return worstPerformer();
        }

        // Transactions
        if (matchesAny(lower, "transaction", "buy", "sell", "recent trade", "trade history", "my trades")) {
            return recentTransactions();
        }

        // Diversification / allocation
        if (matchesAny(lower, "diversif", "allocation", "spread", "sector", "asset type", "balance")) {
            return diversificationAdvice();
        }

        // Risk advice
        if (matchesAny(lower, "risk", "risky", "safe", "volatile", "volatility")) {
            return riskAdvice();
        }

        // Investment suggestions
        if (matchesAny(lower, "suggest", "recommend", "advice", "should i", "what should", "buy more", "rebalance")) {
            return investmentSuggestions();
        }

        // Stock price lookup — detect ticker symbols (2-5 uppercase letters)
        Optional<String> ticker = extractTicker(message);
        if (ticker.isPresent()) {
            return stockInfo(ticker.get());
        }

        // Profit / loss query
        if (matchesAny(lower, "profit", "loss", "gain", "return", "performance", "how am i doing")) {
            return profitLossInfo();
        }

        // Default
        return defaultResponse(message);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private String greeting() {
        return "👋 Hi there! I'm **Pixel AI**, your personal financial portfolio assistant.\n\n" +
               "I can help you with:\n" +
               "• 📊 **Portfolio overview** — 'Show my portfolio'\n" +
               "• 💰 **Holdings** — 'What do I own?'\n" +
               "• 📈 **Stock prices** — 'Price of AAPL'\n" +
               "• 🏆 **Best/worst performers** — 'Which stock is my best?'\n" +
               "• 💡 **Investment suggestions** — 'Give me advice'\n" +
               "• ⚠️ **Risk analysis** — 'How risky is my portfolio?'\n\n" +
               "What would you like to know?";
    }

    private String portfolioSummary() {
        try {
            var summary = portfolioService.getSummary();
            double val = summary.getTotalValue().doubleValue();
            double gl = summary.getTotalGainLoss().doubleValue();
            double glPct = summary.getTotalGainLossPct().doubleValue();
            String arrow = gl >= 0 ? "📈" : "📉";
            String sign = gl >= 0 ? "+" : "";
            return String.format(
                "**Your Portfolio Summary**\n\n" +
                "💼 Total Value: **$%,.2f**\n" +
                "%s Total Gain/Loss: **%s$%,.2f** (%s%.2f%%)\n" +
                "🗂️ Holdings Count: **%d**\n\n" +
                "%s",
                val, arrow, sign, gl, sign, glPct, summary.getHoldingsCount(),
                gl >= 0
                    ? "Your portfolio is in profit! Great work managing your investments. 🎉"
                    : "Your portfolio is currently at a loss. Consider reviewing your holdings and diversification."
            );
        } catch (Exception e) {
            return "I couldn't retrieve your portfolio summary right now. Please try again shortly.";
        }
    }

    private String holdingsSummary() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            if (holdings.isEmpty()) return "You don't have any holdings yet. Add a buy transaction to get started!";

            StringBuilder sb = new StringBuilder("**Your Current Holdings**\n\n");
            long stocks = holdings.stream().filter(h -> "STOCK".equalsIgnoreCase(h.getAssetType())).count();
            long etfs   = holdings.stream().filter(h -> "ETF".equalsIgnoreCase(h.getAssetType())).count();
            long bonds  = holdings.stream().filter(h -> "BOND".equalsIgnoreCase(h.getAssetType())).count();

            sb.append(String.format("📦 Total: **%d positions** (%d stocks, %d ETFs, %d bonds)\n\n", holdings.size(), stocks, etfs, bonds));

            holdings.stream()
                    .sorted(Comparator.comparing(HoldingDto::getMarketValue).reversed())
                    .limit(8)
                    .forEach(h -> {
                        double pct = h.getGainLossPct().doubleValue();
                        String arrow = pct >= 0 ? "▲" : "▼";
                        sb.append(String.format("• **%s** — $%,.2f %s%.1f%%\n",
                                h.getSymbol(), h.getMarketValue().doubleValue(), arrow, Math.abs(pct)));
                    });

            if (holdings.size() > 8) sb.append(String.format("\n...and %d more positions.\n", holdings.size() - 8));
            return sb.toString();
        } catch (Exception e) {
            return "I couldn't load your holdings right now. Please try again.";
        }
    }

    private String bestPerformer() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            if (holdings.isEmpty()) return "No holdings found. Add transactions to track performance!";
            HoldingDto best = holdings.stream()
                    .max(Comparator.comparing(h -> h.getGainLossPct().doubleValue()))
                    .orElseThrow();
            return String.format(
                "🏆 **Best Performer: %s**\n\n" +
                "• Current Price: $%,.2f\n" +
                "• Avg Cost: $%,.2f\n" +
                "• Gain/Loss: **+$%,.2f (+%.2f%%)**\n" +
                "• Market Value: $%,.2f\n\n" +
                "💡 %s is performing well! You might consider taking partial profits if the gain exceeds your target.",
                best.getSymbol(),
                best.getCurrentPrice().doubleValue(),
                best.getAvgCost().doubleValue(),
                best.getGainLoss().doubleValue(),
                best.getGainLossPct().doubleValue(),
                best.getMarketValue().doubleValue(),
                best.getSymbol()
            );
        } catch (Exception e) {
            return "Couldn't calculate best performer. Please try again.";
        }
    }

    private String worstPerformer() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            if (holdings.isEmpty()) return "No holdings found. Add transactions to track performance!";
            HoldingDto worst = holdings.stream()
                    .min(Comparator.comparing(h -> h.getGainLossPct().doubleValue()))
                    .orElseThrow();
            double pct = worst.getGainLossPct().doubleValue();
            String sign = pct < 0 ? "" : "+";
            return String.format(
                "📉 **Worst Performer: %s**\n\n" +
                "• Current Price: $%,.2f\n" +
                "• Avg Cost: $%,.2f\n" +
                "• Gain/Loss: **%s$%,.2f (%s%.2f%%)**\n" +
                "• Market Value: $%,.2f\n\n" +
                "⚠️ %s is underperforming. Consider reviewing the fundamentals — is this a temporary dip or a structural issue?",
                worst.getSymbol(),
                worst.getCurrentPrice().doubleValue(),
                worst.getAvgCost().doubleValue(),
                sign, Math.abs(worst.getGainLoss().doubleValue()),
                sign, Math.abs(pct),
                worst.getMarketValue().doubleValue(),
                worst.getSymbol()
            );
        } catch (Exception e) {
            return "Couldn't calculate worst performer. Please try again.";
        }
    }

    private String recentTransactions() {
        try {
            List<Transaction> all = transactionRepository.findAll();
            if (all.isEmpty()) return "No transactions recorded yet. Use the Transactions page to add buy/sell records.";
            all.sort(Comparator.comparing(Transaction::getExecutedAt).reversed());
            List<Transaction> recent = all.stream().limit(5).toList();
            StringBuilder sb = new StringBuilder("**Recent Transactions (last 5)**\n\n");
            for (Transaction t : recent) {
                sb.append(String.format("• **%s** %s %.4f × $%.2f\n",
                        t.getTxType(), t.getSymbol(),
                        t.getQuantity().doubleValue(), t.getPrice().doubleValue()));
            }
            long buys  = all.stream().filter(t -> "BUY".equalsIgnoreCase(t.getTxType())).count();
            long sells = all.stream().filter(t -> "SELL".equalsIgnoreCase(t.getTxType())).count();
            sb.append(String.format("\n📊 Total: **%d transactions** (%d buys, %d sells)", all.size(), buys, sells));
            return sb.toString();
        } catch (Exception e) {
            return "Couldn't load transactions. Please try again.";
        }
    }

    private String diversificationAdvice() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            if (holdings.isEmpty()) return "No holdings to analyze diversification. Add transactions first.";

            Map<String, Long> typeCounts = holdings.stream()
                    .collect(Collectors.groupingBy(h -> h.getAssetType(), Collectors.counting()));
            Map<String, BigDecimal> typeValues = holdings.stream()
                    .collect(Collectors.groupingBy(h -> h.getAssetType(),
                            Collectors.reducing(BigDecimal.ZERO, HoldingDto::getMarketValue, BigDecimal::add)));

            BigDecimal total = typeValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            StringBuilder sb = new StringBuilder("**Portfolio Allocation & Diversification**\n\n");

            typeValues.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> {
                        double pct = total.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                                : 0;
                        sb.append(String.format("• **%s**: $%,.2f (%.1f%%, %d positions)\n",
                                e.getKey(), e.getValue().doubleValue(), pct, typeCounts.getOrDefault(e.getKey(), 0L)));
                    });

            sb.append("\n💡 **Diversification Tips:**\n");
            if (typeCounts.size() == 1) sb.append("• You hold only one asset type. Consider diversifying across stocks, ETFs, and bonds.\n");
            if (holdings.size() < 5) sb.append("• Your portfolio has few positions. Adding more instruments reduces single-stock risk.\n");
            if (holdings.size() >= 5 && typeCounts.size() >= 2) sb.append("• Your portfolio has reasonable diversification. Keep monitoring allocation drift.\n");

            return sb.toString();
        } catch (Exception e) {
            return "Couldn't analyze diversification. Please try again.";
        }
    }

    private String riskAdvice() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            if (holdings.isEmpty()) return "No holdings to assess risk. Add transactions first!";
            BigDecimal total = holdings.stream().map(HoldingDto::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            Optional<HoldingDto> largest = holdings.stream().max(Comparator.comparing(h -> h.getMarketValue().doubleValue()));
            double concentration = largest.map(h -> total.compareTo(BigDecimal.ZERO) > 0
                    ? h.getMarketValue().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0d).orElse(0d);

            StringBuilder sb = new StringBuilder("**Portfolio Risk Overview**\n\n");
            if (concentration > 40) {
                sb.append(String.format("⚠️ **Concentration Risk**: %s represents %.1f%% of your portfolio — that's high! Consider trimming.\n",
                        largest.get().getSymbol(), concentration));
            } else {
                sb.append(String.format("✅ Largest position (%s) is %.1f%% of total — reasonably sized.\n",
                        largest.get().getSymbol(), concentration));
            }

            long negCount = holdings.stream().filter(h -> h.getGainLoss().doubleValue() < 0).count();
            if (negCount > 0) sb.append(String.format("📉 %d of your %d positions are at a loss.\n", negCount, holdings.size()));

            sb.append("\n💡 **Risk Management Tips:**\n");
            sb.append("• Diversify across sectors and asset classes\n");
            sb.append("• Use stop-loss orders to limit downside\n");
            sb.append("• Review the Risk Analysis page for per-symbol volatility, Sharpe ratio & beta\n");
            sb.append("• Never invest more than you can afford to lose\n");
            return sb.toString();
        } catch (Exception e) {
            return "Couldn't assess portfolio risk. Please try again.";
        }
    }

    private String investmentSuggestions() {
        try {
            List<HoldingDto> holdings = portfolioService.getHoldings();
            StringBuilder sb = new StringBuilder("**Investment Suggestions**\n\n");

            if (holdings.isEmpty()) {
                return "💡 No holdings yet! Consider starting with diversified ETFs (like SPY, QQQ) which spread risk across many companies.";
            }

            sb.append("Based on your current portfolio:\n\n");

            // Check diversification
            long typeCnt = holdings.stream().map(HoldingDto::getAssetType).distinct().count();
            if (typeCnt == 1 && "STOCK".equalsIgnoreCase(holdings.get(0).getAssetType())) {
                sb.append("📌 **Add ETFs** — you hold only stocks. ETFs like SPY or VTI reduce volatility.\n");
            }

            if (holdings.size() < 5) {
                sb.append("📌 **Diversify** — fewer than 5 positions increases concentration risk.\n");
            }

            // Best performers
            Optional<HoldingDto> best = holdings.stream().max(Comparator.comparing(h -> h.getGainLossPct().doubleValue()));
            best.ifPresent(h -> {
                if (h.getGainLossPct().doubleValue() > 20) {
                    sb.append(String.format("📌 **Take profits** — %s is up %.1f%%. Consider locking in some gains.\n",
                            h.getSymbol(), h.getGainLossPct().doubleValue()));
                }
            });

            // Worst performers
            Optional<HoldingDto> worst = holdings.stream().min(Comparator.comparing(h -> h.getGainLossPct().doubleValue()));
            worst.ifPresent(h -> {
                if (h.getGainLossPct().doubleValue() < -15) {
                    sb.append(String.format("📌 **Review %s** — it's down %.1f%%. Is it worth holding or cutting losses?\n",
                            h.getSymbol(), Math.abs(h.getGainLossPct().doubleValue())));
                }
            });

            sb.append("\n⚠️ *This is educational content only — not financial advice. Always consult a licensed advisor.*");
            return sb.toString();
        } catch (Exception e) {
            return "Couldn't generate suggestions right now. Please try again.";
        }
    }

    private String stockInfo(String symbol) {
        try {
            QuoteDto quote = marketDataService.getQuote(symbol);
            if (quote == null || quote.getCurrent() == null) {
                return String.format("I couldn't find live data for **%s**. Make sure it's a valid ticker symbol.", symbol);
            }
            double price = quote.getCurrent().doubleValue();
            double change = quote.getChange() != null ? quote.getChange().doubleValue() : 0;
            double changePct = quote.getChangePercent() != null ? quote.getChangePercent().doubleValue() : 0;
            String arrow = changePct >= 0 ? "📈" : "📉";
            String sign = changePct >= 0 ? "+" : "";

            StringBuilder sb = new StringBuilder(String.format(
                "%s **%s — Live Quote**\n\n" +
                "• Price: **$%,.2f**\n" +
                "• Change: **%s$%.2f (%s%.2f%%)**\n",
                arrow, symbol.toUpperCase(), price, sign, Math.abs(change), sign, Math.abs(changePct)
            ));

            if (quote.getHigh() != null && quote.getLow() != null) {
                sb.append(String.format("• Day Range: $%.2f — $%.2f\n", quote.getLow().doubleValue(), quote.getHigh().doubleValue()));
            }

            // Check if user holds this
            List<HoldingDto> holdings = portfolioService.getHoldings();
            holdings.stream().filter(h -> symbol.equalsIgnoreCase(h.getSymbol())).findFirst().ifPresent(h -> {
                sb.append(String.format("\n💼 You hold **%.4f shares** at avg cost $%.2f (P&L: %s$%.2f)",
                        h.getQuantity().doubleValue(), h.getAvgCost().doubleValue(),
                        h.getGainLoss().doubleValue() >= 0 ? "+" : "", h.getGainLoss().doubleValue()));
            });

            return sb.toString();
        } catch (Exception e) {
            return String.format("I couldn't retrieve data for **%s** right now. Please try again.", symbol);
        }
    }

    private String profitLossInfo() {
        try {
            var summary = portfolioService.getSummary();
            double gl = summary.getTotalGainLoss().doubleValue();
            double glPct = summary.getTotalGainLossPct().doubleValue();
            String emoji = gl >= 0 ? "🟢" : "🔴";
            String sign = gl >= 0 ? "+" : "";
            return String.format(
                "%s **Portfolio P&L**\n\n" +
                "• Total Value: **$%,.2f**\n" +
                "• Total Cost Basis: **$%,.2f**\n" +
                "• Net Gain/Loss: **%s$%,.2f (%s%.2f%%)**\n\n" +
                "%s",
                emoji,
                summary.getTotalValue().doubleValue(),
                summary.getTotalCost().doubleValue(),
                sign, gl, sign, glPct,
                gl >= 0 ? "Your investments are profitable. Keep up the disciplined approach! 👍" : "Your portfolio is below cost basis. Review your strategy and consider rebalancing."
            );
        } catch (Exception e) {
            return "Couldn't load P&L data. Please try again.";
        }
    }

    private String defaultResponse(String message) {
        return String.format(
            "I'm not sure I understood **\"%s\"**.\n\n" +
            "Try asking:\n" +
            "• 'Show my portfolio'\n" +
            "• 'What are my holdings?'\n" +
            "• 'Price of AAPL'\n" +
            "• 'Which stock is my best performer?'\n" +
            "• 'Give me investment advice'\n" +
            "• 'How risky is my portfolio?'",
            message.length() > 50 ? message.substring(0, 50) + "..." : message
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean matchesAny(String lower, String... keywords) {
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private Optional<String> extractTicker(String message) {
        // Match standalone 2-5 uppercase letter tokens (likely tickers)
        Pattern p = Pattern.compile("\\b([A-Z]{2,5})\\b");
        Matcher m = p.matcher(message);
        // Skip common English words that happen to be uppercase
        Set<String> skip = Set.of("I", "MY", "THE", "AND", "OR", "FOR", "HOW", "AM", "IS", "IT", "IN", "AT",
                "OF", "BE", "DO", "TO", "UP", "ON", "BY", "AN", "IF", "NO", "GO", "HI", "OK", "ALL",
                "BUY", "SELL", "GIVE", "SHOW", "GET", "WHAT", "WHICH", "CAN", "YOU", "ME", "US");
        while (m.find()) {
            String candidate = m.group(1);
            if (!skip.contains(candidate) && candidate.length() >= 2 && candidate.length() <= 5) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
