package com.jpb.reconciliation.reconciliation.util;

import com.jpb.reconciliation.reconciliation.dto.EjRawTransactionBlock;
import com.jpb.reconciliation.reconciliation.dto.EjTransaction;
import com.jpb.reconciliation.reconciliation.parser.EjTransactionParser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class EjTransactionParserTest {

    private final EjTransactionParser parser =
            new EjTransactionParser("test-batch-001", "EFNJ");

    @Test
    public void testWithdrawalParsedCorrectly() {
        List<String> lines = Arrays.asList(
            "[020t*478*04/02/2025*07:37:21*",
            "*TRANSACTION START*",
            "CARD INSERTED",
            "CARD: 606998******2840",
            "PIN ENTERED",
            "OPCODE = F",
            "REQUEST SENT [AMOUNT=00001000]",
            "RESPONSE RECEIVED [FUNCTION ID=B,  TXN SN NO=6436]",
            "02-APR-2025      07:37   EFNJ000389010",
            "CARD NUMBER 606998XXXXXX2840",
            "TXN NO 6436",
            "REFERENCE NO 123456",
            "RESPONSE CODE 000",
            "WITHDRAWAL    RS.    1000.00",
            "AVAIL BAL     RS.    8545.48",
            "TRANSACTION END"
        );

        EjRawTransactionBlock block =
                new EjRawTransactionBlock("test.txt", 1, 16, lines);

        EjTransaction txn = parser.parse(block);

        // Type & Status
        assertEquals(EjTransaction.Type.WITHDRAWAL, txn.getTransactionType());
        assertEquals(EjTransaction.Status.SUCCESS, txn.getTransactionStatus());

        // Fields
        assertEquals("EFNJ000389010", txn.getAtmId());
        assertEquals("6436", txn.getTxnNo());
        assertEquals("000", txn.getResponseCode());
        assertNotNull(txn.getWithdrawalAmount());
        assertNotNull(txn.getAvailBalance());

        // Flags
        assertTrue(txn.isPinEntered());
        assertTrue(txn.isCardInserted());

        System.out.println("✅ Withdrawal test passed: " + txn);
    }

    @Test
    public void testFailedTransactionParsedCorrectly() {
        List<String> lines = Arrays.asList(
            "[020t*479*04/02/2025*07:45:00*",
            "*TRANSACTION START*",
            "CARD INSERTED",
            "PIN ENTERED",
            "OPCODE = F",
            "UNABLE TO PROCESS",
            "TRANSACTION END"
        );

        EjRawTransactionBlock block =
                new EjRawTransactionBlock("test.txt", 17, 23, lines);

        EjTransaction txn = parser.parse(block);

        assertEquals(EjTransaction.Type.FAILED, txn.getTransactionType());
        assertEquals(EjTransaction.Status.FAILED, txn.getTransactionStatus());

        System.out.println("✅ Failed txn test passed: " + txn);
    }

    @Test
    public void testCustomerCancelledParsedCorrectly() {
        List<String> lines = Arrays.asList(
            "[020t*480*04/02/2025*07:50:00*",
            "*TRANSACTION START*",
            "CARD INSERTED",
            "PIN ENTERED",
            "CUSTOMER CANCELLED",
            "TRANSACTION END"
        );

        EjRawTransactionBlock block =
                new EjRawTransactionBlock("test.txt", 24, 29, lines);

        EjTransaction txn = parser.parse(block);

        assertEquals(EjTransaction.Status.CUSTOMER_CANCELLED, txn.getTransactionStatus());

        System.out.println("✅ Customer cancelled test passed: " + txn);
    }

    @Test
    public void testEmptyBlockDoesNotCrash() {
        List<String> lines = Arrays.asList(
            "*TRANSACTION START*",
            "TRANSACTION END"
        );

        EjRawTransactionBlock block =
                new EjRawTransactionBlock("test.txt", 1, 2, lines);

        EjTransaction txn = parser.parse(block);

        // Should not throw, should return UNKNOWN/OTHER
        assertNotNull(txn);
        assertNotNull(txn.getTransactionType());
        assertNotNull(txn.getTransactionStatus());

        System.out.println("✅ Empty block test passed: " + txn);
    }
}