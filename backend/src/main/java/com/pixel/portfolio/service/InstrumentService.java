package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.InstrumentDto;
import com.pixel.portfolio.dto.PricePointDto;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.util.PeriodUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public InstrumentService(InstrumentRepository instrumentRepository, PriceHistoryRepository priceHistoryRepository) {
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public List<InstrumentDto> listInstruments() {
        return instrumentRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<PricePointDto> getPriceSeries(String symbol, String period) {
        String sym = symbol.toUpperCase();
        if (!instrumentRepository.existsById(sym)) {
            throw new ResourceNotFoundException("Unknown instrument symbol: " + sym);
        }
        LocalDate startDate = PeriodUtil.startDateFor(period, LocalDate.now());
        List<PriceHistory> rows = startDate == null
                ? priceHistoryRepository.findBySymbolOrderByTradeDateAsc(sym)
                : priceHistoryRepository.findBySymbolAndTradeDateGreaterThanEqualOrderByTradeDateAsc(sym, startDate);
        return rows.stream()
                .map(r -> new PricePointDto(r.getTradeDate(), r.getOpen(), r.getHigh(), r.getLow(), r.getClose(), r.getVolume()))
                .toList();
    }

    private InstrumentDto toDto(Instrument i) {
        return new InstrumentDto(i.getSymbol(), i.getName(), i.getAssetType(), i.getCurrency());
    }
}
