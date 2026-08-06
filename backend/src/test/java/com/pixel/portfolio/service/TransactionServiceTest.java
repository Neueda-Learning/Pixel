package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.LotDto;
import com.pixel.portfolio.dto.TransactionRequestDto;
import com.pixel.portfolio.dto.TransactionResponseDto;
import com.pixel.portfolio.exception.BadRequestException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.Transaction;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock InstrumentRepository instrumentRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock TwelveDataHistoricalService twelveDataService;

    @InjectMocks TransactionService transactionService;

    private static Transaction tx(long id, String symbol, String type, double qty, double price, Instant when) {
        Transaction t = new Transaction();
        setId(t, id);
        t.setSymbol(symbol);
        t.setTxType(type);
        t.setQuantity(BigDecimal.valueOf(qty));
        t.setPrice(BigDecimal.valueOf(price));
        t.setFees(BigDecimal.ZERO);
        t.setExecutedAt(when);
        return t;
    }

    private static Transaction sellTx(long id, String symbol, double qty, double price, Long buyTransactionId, Instant when) {
        Transaction t = tx(id, symbol, "SELL", qty, price, when);
        t.setBuyTransactionId(buyTransactionId);
        return t;
    }

    /** Transaction#id has no public setter (JPA-generated), so tests assign it via reflection. */
    private static void setId(Transaction t, long id) {
        try {
            Field f = Transaction.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static TransactionRequestDto request(String symbol, String type, double qty, double price,
                                                  BigDecimal buyPrice, Long buyTransactionId, Instant when) {
        TransactionRequestDto r = new TransactionRequestDto();
        r.setSymbol(symbol);
        r.setTxType(type);
        r.setQuantity(BigDecimal.valueOf(qty));
        r.setPrice(BigDecimal.valueOf(price));
        r.setBuyPrice(buyPrice);
        r.setBuyTransactionId(buyTransactionId);
        r.setExecutedAt(when);
        return r;
    }

    /** Makes transactionRepository.save() an identity function (assigns an id when missing) so createFrom() can return it. */
    private void stubSaveReturnsSameEntityWithId(long nextId) {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) setId(t, nextId);
            return t;
        });
    }

    /**
     * Installs a stateful in-memory fake over transactionRepository (findAll/findById/save) backed by a
     * growing list, so multi-step flows like importAll() see each previously-saved row when validating
     * the next one — mirroring how the real JPA repository behaves within a request.
     */
    private List<Transaction> installStatefulRepository() {
        List<Transaction> store = new ArrayList<>();
        AtomicLong idSeq = new AtomicLong(1);
        org.mockito.Mockito.lenient().when(transactionRepository.findAll()).thenAnswer(inv -> new ArrayList<>(store));
        org.mockito.Mockito.lenient().when(transactionRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return store.stream().filter(t -> id.equals(t.getId())).findFirst();
        });
        org.mockito.Mockito.lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) setId(t, idSeq.getAndIncrement());
            store.add(t);
            return t;
        });
        return store;
    }

    // ---- add(): BUY ----

    @Test
    void add_buy_savesWithNullBuyPriceAndBuyTransactionId() {
        stubSaveReturnsSameEntityWithId(1L);
        TransactionRequestDto req = request("AAPL", "BUY", 10, 150.00, null, null, Instant.now());

        TransactionResponseDto dto = transactionService.add(req);

        assertEquals("AAPL", dto.getSymbol());
        assertEquals("BUY", dto.getTxType());
        assertNull(dto.getBuyPrice());
        assertNull(dto.getBuyTransactionId());
    }

    // ---- add(): SELL validation ----

    @Test
    void add_sellExceedingTotalHoldings_throwsBadRequestBeforeLotCheck() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 5, 150.00, day1)
        ));
        TransactionRequestDto req = request("AAPL", "SELL", 10, 200.00, BigDecimal.valueOf(150), null, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertTrue(ex.getMessage().contains("only 5 currently held"));
    }

    @Test
    void add_sellWithoutBuyPriceOrLot_throwsBadRequest() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1)
        ));
        TransactionRequestDto req = request("AAPL", "SELL", 5, 200.00, null, null, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertEquals("buyPrice is required for SELL transactions", ex.getMessage());
    }

    @Test
    void add_sellWithManualBuyPrice_setsBuyPriceAndNullLot() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1)
        ));
        stubSaveReturnsSameEntityWithId(2L);
        TransactionRequestDto req = request("AAPL", "SELL", 5, 200.00, BigDecimal.valueOf(150), null, Instant.now());

        TransactionResponseDto dto = transactionService.add(req);

        assertEquals(0, BigDecimal.valueOf(150).compareTo(dto.getBuyPrice()));
        assertNull(dto.getBuyTransactionId());
    }

    @Test
    void add_sellWithBuyTransactionId_derivesPriceAndLotFromReferencedBuy() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        Transaction buyLot = tx(1, "AAPL", "BUY", 10, 150.00, day1);
        when(transactionRepository.findAll()).thenReturn(List.of(buyLot));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(buyLot));
        stubSaveReturnsSameEntityWithId(2L);
        TransactionRequestDto req = request("AAPL", "SELL", 4, 200.00, null, 1L, Instant.now());

        TransactionResponseDto dto = transactionService.add(req);

        assertEquals(0, BigDecimal.valueOf(150).compareTo(dto.getBuyPrice()));
        assertEquals(1L, dto.getBuyTransactionId());
    }

    @Test
    void add_sellExceedingSpecificLotRemaining_throwsBadRequestWithRemainingMessage() {
        Instant day1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(10, ChronoUnit.DAYS);
        Transaction lotA = tx(1, "AAPL", "BUY", 10, 150.00, day1);
        Transaction lotB = tx(2, "AAPL", "BUY", 20, 160.00, day2);
        // Total held (30) is enough overall, but lot A only has 10 remaining.
        when(transactionRepository.findAll()).thenReturn(List.of(lotA, lotB));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(lotA));
        TransactionRequestDto req = request("AAPL", "SELL", 15, 200.00, null, 1L, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertEquals("Cannot sell 15 share(s) from this lot — only 10 remaining.", ex.getMessage());
    }

    @Test
    void add_sellReferencingLotOfDifferentSymbol_throwsBadRequest() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        Transaction otherSymbolLot = tx(1, "MSFT", "BUY", 10, 300.00, day1);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(2, "AAPL", "BUY", 10, 150.00, day1), otherSymbolLot
        ));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(otherSymbolLot));
        TransactionRequestDto req = request("AAPL", "SELL", 5, 200.00, null, 1L, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertEquals("Selected buy lot does not match this symbol", ex.getMessage());
    }

    @Test
    void add_sellReferencingSellTransactionAsLot_throwsBadRequest() {
        Instant day1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(10, ChronoUnit.DAYS);
        Transaction priorSell = sellTx(1, "AAPL", 3, 180.00, null, day2);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(2, "AAPL", "BUY", 10, 150.00, day1), priorSell
        ));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(priorSell));
        TransactionRequestDto req = request("AAPL", "SELL", 2, 200.00, null, 1L, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertEquals("Selected buy lot does not match this symbol", ex.getMessage());
    }

    @Test
    void add_sellReferencingNonexistentLot_throwsBadRequest() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(2, "AAPL", "BUY", 10, 150.00, day1)
        ));
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        TransactionRequestDto req = request("AAPL", "SELL", 5, 200.00, null, 99L, Instant.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.add(req));
        assertEquals("Selected buy lot not found", ex.getMessage());
    }

    // ---- getOpenLots() ----

    @Test
    void getOpenLots_tracksRemainingQuantityAfterExplicitLotSell() {
        Instant day1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day3 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1),
                tx(2, "AAPL", "BUY", 5, 160.00, day2),
                sellTx(3, "AAPL", 3, 200.00, 1L, day3)
        ));

        List<LotDto> lots = transactionService.getOpenLots("AAPL", null);

        assertEquals(2, lots.size());
        assertEquals(1L, lots.get(0).getTransactionId());
        assertEquals(0, BigDecimal.valueOf(7).compareTo(lots.get(0).getRemainingQuantity()));
        assertEquals(2L, lots.get(1).getTransactionId());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(lots.get(1).getRemainingQuantity()));
    }

    @Test
    void getOpenLots_fifoFallbackConsumesOldestLotsFirstForUnassignedSell() {
        Instant day1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day3 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1),
                tx(2, "AAPL", "BUY", 5, 160.00, day2),
                sellTx(3, "AAPL", 12, 200.00, null, day3) // no lot reference: CSV-import style
        ));

        List<LotDto> lots = transactionService.getOpenLots("AAPL", null);

        // 12 consumed FIFO: all 10 from lot 1, then 2 from lot 2 -> lot 1 fully depleted (excluded), lot 2 has 3 left
        assertEquals(1, lots.size());
        assertEquals(2L, lots.get(0).getTransactionId());
        assertEquals(0, BigDecimal.valueOf(3).compareTo(lots.get(0).getRemainingQuantity()));
    }

    @Test
    void getOpenLots_excludeTransactionId_restoresThatTransactionsConsumedQuantity() {
        Instant day1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(10, ChronoUnit.DAYS);
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1),
                sellTx(2, "AAPL", 4, 200.00, 1L, day2)
        ));

        List<LotDto> lots = transactionService.getOpenLots("AAPL", 2L);

        assertEquals(1, lots.size());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(lots.get(0).getRemainingQuantity()));
    }

    // ---- update() ----

    @Test
    void update_existingSellTransaction_recomputesBuyPricingFromNewLot() {
        Instant day1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(15, ChronoUnit.DAYS);
        Instant day3 = Instant.now().minus(5, ChronoUnit.DAYS);
        Transaction existingSell = sellTx(3, "AAPL", 2, 190.00, null, day3);
        existingSell.setBuyPrice(BigDecimal.valueOf(150));
        Transaction newLot = tx(2, "AAPL", "BUY", 8, 160.00, day2);
        when(transactionRepository.findById(3L)).thenReturn(Optional.of(existingSell));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(newLot));
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, day1), newLot, existingSell
        ));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        TransactionRequestDto req = request("AAPL", "SELL", 2, 195.00, null, 2L, Instant.now());

        TransactionResponseDto dto = transactionService.update(3L, req);

        assertEquals(0, BigDecimal.valueOf(160).compareTo(dto.getBuyPrice()));
        assertEquals(2L, dto.getBuyTransactionId());
    }

    @Test
    void update_changingSellBackToBuy_clearsBuyPriceAndLot() {
        Instant day1 = Instant.now().minus(10, ChronoUnit.DAYS);
        Transaction existingSell = sellTx(1, "AAPL", 2, 190.00, null, day1);
        existingSell.setBuyPrice(BigDecimal.valueOf(150));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existingSell));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        TransactionRequestDto req = request("AAPL", "BUY", 2, 190.00, null, null, Instant.now());

        TransactionResponseDto dto = transactionService.update(1L, req);

        assertEquals("BUY", dto.getTxType());
        assertNull(dto.getBuyPrice());
        assertNull(dto.getBuyTransactionId());
    }

    @Test
    void update_nonexistentTransaction_throwsResourceNotFound() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());
        TransactionRequestDto req = request("AAPL", "BUY", 1, 100.00, null, null, Instant.now());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.update(404L, req));
    }

    // ---- delete() ----

    @Test
    void delete_nonexistentTransaction_throwsResourceNotFound() {
        when(transactionRepository.existsById(404L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> transactionService.delete(404L));
    }

    @Test
    void delete_existingTransaction_removesIt() {
        when(transactionRepository.existsById(1L)).thenReturn(true);

        transactionService.delete(1L);

        org.mockito.Mockito.verify(transactionRepository).deleteById(1L);
    }

    // ---- importAll(): sequential ordering ----

    @Test
    void importAll_processesRowsInOrder_allowingSellAfterEarlierBuyInSameBatch() {
        installStatefulRepository();
        Instant day1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(10, ChronoUnit.DAYS);
        List<TransactionRequestDto> batch = List.of(
                request("AAPL", "BUY", 10, 150.00, null, null, day1),
                request("AAPL", "SELL", 5, 200.00, BigDecimal.valueOf(150), null, day2)
        );

        List<TransactionResponseDto> results = transactionService.importAll(batch);

        assertEquals(2, results.size());
        assertEquals("BUY", results.get(0).getTxType());
        assertEquals("SELL", results.get(1).getTxType());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(results.get(1).getBuyPrice()));
    }

    @Test
    void importAll_sellBeforeCorrespondingBuyInBatch_throwsBadRequest() {
        installStatefulRepository();
        List<TransactionRequestDto> batch = List.of(
                request("AAPL", "SELL", 5, 200.00, BigDecimal.valueOf(150), null, Instant.now())
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transactionService.importAll(batch));
        assertTrue(ex.getMessage().contains("only 0 currently held"));
    }

    // ---- list() ----

    @Test
    void list_customDateRange_sortsResultsNewestFirst() {
        Instant older = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant newer = Instant.now().minus(1, ChronoUnit.DAYS);
        when(transactionRepository.findByExecutedAtBetween(any(), any())).thenReturn(List.of(
                tx(1, "AAPL", "BUY", 10, 150.00, older),
                tx(2, "MSFT", "BUY", 3, 300.00, newer)
        ));

        List<TransactionResponseDto> result = transactionService.list(
                "ALL", java.time.LocalDate.now().minusDays(10), java.time.LocalDate.now());

        assertEquals(2, result.size());
        assertEquals("MSFT", result.get(0).getSymbol());
        assertEquals("AAPL", result.get(1).getSymbol());
    }
}
