package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.InstrumentDto;
import com.pixel.portfolio.dto.PricePointDto;
import com.pixel.portfolio.service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@Tag(name = "Instruments", description = "Reference data and historical price series")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    @Operation(summary = "List known instruments", description = "All instruments with reference data (symbol, name, asset type, currency).")
    public List<InstrumentDto> listInstruments() {
        return instrumentService.listInstruments();
    }

    @GetMapping("/{symbol}/prices")
    @Operation(summary = "Get historical price series", description = "Daily OHLCV price history for a symbol, for the given period, sourced from price_history.")
    public List<PricePointDto> getPrices(
            @PathVariable String symbol,
            @Parameter(description = "1M, 3M, 6M, 1Y, or ALL") @RequestParam(defaultValue = "6M") String period) {
        return instrumentService.getPriceSeries(symbol, period);
    }
}
