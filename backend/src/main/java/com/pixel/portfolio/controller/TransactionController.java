package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponseDto> history(@RequestParam(defaultValue = "ALL") String period) {
        return transactionService.list(period);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDto add(@Valid @RequestBody TransactionRequestDto request) {
        return transactionService.add(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }
}
