package dev.qrm.api.economy;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;
import org.junit.jupiter.api.Test;

class TransferRequestTest {
    private final AccountId a = AccountId.random();
    private final AccountId b = AccountId.random();

    private TransferRequest.Builder valid() {
        return TransferRequest.builder().from(a).to(b).amount(Money.of("USD", 100)).reason("test").idempotencyKey("k1");
    }

    @Test
    void builderAppliesDefaults() {
        TransferRequest r = valid().build();
        assertEquals("{}", r.metadataJson());
        assertNull(r.groupId());
    }

    @Test
    void missingFieldRejected() {
        assertThrows(NullPointerException.class, () -> TransferRequest.builder().from(a).to(b).build());
    }

    @Test
    void blankReasonOrKeyRejected() {
        assertThrows(IllegalArgumentException.class, () -> valid().reason(" ").build());
        assertThrows(IllegalArgumentException.class, () -> valid().idempotencyKey("").build());
    }

    @Test
    void tooLongKeyRejected() {
        assertThrows(IllegalArgumentException.class, () -> valid().idempotencyKey("x".repeat(129)).build());
    }

    @Test
    void withAmountReplacesOnlyAmount() {
        TransferRequest r = valid().build().withAmount(Money.of("USD", 50));
        assertEquals(Money.of("USD", 50), r.amount());
        assertEquals("k1", r.idempotencyKey());
    }
}
