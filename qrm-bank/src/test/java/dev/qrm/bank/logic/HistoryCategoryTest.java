package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HistoryCategoryTest {
    @Test
    void bankReasonsKeepTheirCategory() {
        assertEquals(HistoryCategory.WITHDRAW, HistoryCategory.of("bank.withdraw", -100));
        assertEquals(HistoryCategory.DEPOSIT, HistoryCategory.of("bank.deposit", 100));
        assertEquals(HistoryCategory.REVERT, HistoryCategory.of("bank.deposit.revert", 100));
        assertEquals(HistoryCategory.ADMIN, HistoryCategory.of("admin.take", -5));
    }

    @Test
    void transfersFollowTheSign() {
        assertEquals(HistoryCategory.TRANSFER_IN, HistoryCategory.of("bank.transfer", 10));
        assertEquals(HistoryCategory.TRANSFER_OUT, HistoryCategory.of("player.pay", -10));
    }

    @Test
    void otherReasonsAreRecognisedByPrefix() {
        assertEquals(HistoryCategory.WORK, HistoryCategory.of("salary", 1200));
        assertEquals(HistoryCategory.WORK, HistoryCategory.of("job.payroll", 1200));
        assertEquals(HistoryCategory.SHOP, HistoryCategory.of("shop.buy", -50));
        assertEquals(HistoryCategory.FUEL, HistoryCategory.of("fuel_pump", -30));
        assertEquals(HistoryCategory.OTHER, HistoryCategory.of("shopping_spree_x", -1));
        assertEquals(HistoryCategory.OTHER, HistoryCategory.of("", 1));
        assertEquals(HistoryCategory.OTHER, HistoryCategory.of(null, 1));
    }
}
