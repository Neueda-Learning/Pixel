package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "The buy/sell ledger that holdings are derived from")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "List transactions", description = "Transaction history, optionally filtered to the given period (3M, 6M, 1Y, or ALL), or to a custom [from, to] date range which takes precedence over period.")
    public List<TransactionResponseDto> history(
            @Parameter(description = "3M, 6M, 1Y, or ALL") @RequestParam(defaultValue = "ALL") String period,
            @Parameter(description = "Custom range start date (inclusive), overrides period when set")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Custom range end date (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.list(period, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a transaction", description = "Records a buy or sell. Holdings and portfolio value are derived from these automatically.")
    public TransactionResponseDto add(@Valid @RequestBody TransactionRequestDto request) {
        return transactionService.add(request);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bulk import transactions", description = "Imports a batch of historical transactions (e.g. parsed from a CSV) in one call.")
    public List<TransactionResponseDto> importAll(@Valid @RequestBody List<@Valid TransactionRequestDto> requests) {
        return transactionService.importAll(requests);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a transaction", description = "Updates an existing (e.g. imported/historical) transaction.")
    public TransactionResponseDto update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDto request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a transaction", description = "Removes a transaction from the ledger.")
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }
}
