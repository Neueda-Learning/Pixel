package com.pixel.portfolio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.pixel.portfolio.dto.HoldingResponse;
import com.pixel.portfolio.dto.HoldingWeightResponse;
import com.pixel.portfolio.dto.PortfolioDiversityResponse;
import com.pixel.portfolio.dto.PortfolioPnLResponse;

public final class CsvUtil {

	private CsvUtil() {
	}

	public static String portfolioStatementCsv(PortfolioPnLResponse pnl) {
		StringBuilder csv = new StringBuilder();
		append(csv, "Portfolio Id", "Portfolio Name", "Cash Balance", "Cost Basis", "Current Market Value", "Total Value", "Total PnL", "PnL %");
		append(csv,
				value(pnl.getPortfolioId()),
				value(pnl.getPortfolioName()),
				value(pnl.getCashBalance()),
				value(pnl.getCostBasis()),
				value(pnl.getCurrentMarketValue()),
				value(pnl.getTotalPortfolioValue()),
				value(pnl.getTotalPnl()),
				value(pnl.getPnlPercentage()));
		csv.append("\nHoldings\n");
		append(csv, "Symbol", "Quantity", "Average Price", "Market Price", "Market Value", "PnL");
		for (HoldingResponse holding : pnl.getHoldings()) {
			append(csv,
					value(holding.getSymbol()),
					value(holding.getQuantity()),
					value(holding.getAveragePrice()),
					value(holding.getMarketPrice()),
					value(holding.getMarketValue()),
					value(holding.getPnl()));
		}
		return csv.toString();
	}

	public static String diversityCsv(PortfolioDiversityResponse diversity) {
		StringBuilder csv = new StringBuilder();
		append(csv, "Portfolio Id", "Portfolio Name", "HHI", "Diversity Score", "Top Holding Weight", "Concentration Level");
		append(csv,
				value(diversity.getPortfolioId()),
				value(diversity.getPortfolioName()),
				value(diversity.getHerfindahlIndex()),
				value(diversity.getDiversityScore()),
				value(diversity.getTopHoldingWeight()),
				value(diversity.getConcentrationLevel()));
		csv.append("\nHoldings\n");
		append(csv, "Symbol", "Weight", "Market Value");
		for (HoldingWeightResponse holding : diversity.getHoldings()) {
			append(csv, value(holding.getSymbol()), value(holding.getWeight()), value(holding.getMarketValue()));
		}
		return csv.toString();
	}

	private static void append(StringBuilder csv, String... values) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				csv.append(',');
			}
			csv.append(escape(values[i]));
		}
		csv.append('\n');
	}

	private static String value(Object value) {
		return value == null ? "" : value.toString();
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		String sanitized = value.replace("\r", " ").replace("\n", " ");
		if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@")) {
			sanitized = "'" + sanitized;
		}
		if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("'")) {
			sanitized = '"' + sanitized.replace("\"", "\"\"") + '"';
		}
		return sanitized;
	}

	public static BigDecimal scaleMoney(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return value.setScale(4, RoundingMode.HALF_UP);
	}
}

