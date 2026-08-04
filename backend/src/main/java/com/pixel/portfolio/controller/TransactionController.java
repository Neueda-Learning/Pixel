package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "List transactions", description = "Transaction history, optionally filtered to the given period (3M, 6M, 1Y, or ALL).")
    public List<TransactionResponseDto> history(
            @Parameter(description = "3M, 6M, 1Y, or ALL") @RequestParam(defaultValue = "ALL") String period) {
        return transactionService.list(period);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a transaction", description = "Records a buy or sell. Holdings and portfolio value are derived from these automatically.")
    public TransactionResponseDto add(@Valid @RequestBody TransactionRequestDto request) {
        return transactionService.add(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a transaction", description = "Removes a transaction from the ledger.")
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }
}
