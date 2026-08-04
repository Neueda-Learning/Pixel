package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.InstrumentDto;
import com.pixel.portfolio.dto.PricePointDto;
import com.pixel.portfolio.service.InstrumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<InstrumentDto> listInstruments() {
        return instrumentService.listInstruments();
    }

    @GetMapping("/{symbol}/prices")
    public List<PricePointDto> getPrices(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "6M") String period) {
        return instrumentService.getPriceSeries(symbol, period);
    }
}
