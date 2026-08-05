package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import com.pixel.portfolio.util.PeriodUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InstrumentRepository instrumentRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               InstrumentRepository instrumentRepository) {
        this.transactionRepository = transactionRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public List<TransactionResponseDto> list(String period) {
        LocalDate startDate = PeriodUtil.startDateFor(period, LocalDate.now());
        List<Transaction> txs = startDate == null
                ? transactionRepository.findAll()
                : transactionRepository.findByExecutedAtAfter(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        return txs.stream()
                .sorted(Comparator.comparing(Transaction::getExecutedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public TransactionResponseDto add(TransactionRequestDto request) {
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (!instrumentRepository.existsById(symbol)) {
            instrumentRepository.save(new Instrument(symbol, symbol, "STOCK", "USD"));
        }
        Transaction tx = new Transaction();
        tx.setSymbol(request.getSymbol().trim().toUpperCase(Locale.ROOT));
        tx.setTxType(request.getTxType().toUpperCase(Locale.ROOT));
        tx.setQuantity(request.getQuantity());
        tx.setPrice(request.getPrice());
        tx.setFees(request.getFees() != null ? request.getFees() : BigDecimal.ZERO);
        tx.setExecutedAt(request.getExecutedAt() != null ? request.getExecutedAt() : Instant.now());
        tx.setNotes(request.getNotes());
        return toDto(transactionRepository.save(tx));
    }

    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }
        transactionRepository.deleteById(id);
    }

    private TransactionResponseDto toDto(Transaction t) {
        return new TransactionResponseDto(t.getId(), t.getSymbol(), t.getTxType(), t.getQuantity(),
                t.getPrice(), t.getFees(), t.getExecutedAt(), t.getNotes());
    }
}
