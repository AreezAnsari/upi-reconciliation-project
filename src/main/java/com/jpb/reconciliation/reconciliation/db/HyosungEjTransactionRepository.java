package com.jpb.reconciliation.reconciliation.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.hyosung.model.EjTransaction;

import javax.sql.DataSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * JDBC-based repository for Hyosung {@link EjTransaction}.
 *
 * <p>Inserts one batch of parsed Hyosung EJ transactions into
 * {@code atm_ej_transaction_hyosung} table in a single DB transaction.
 * On failure the batch is rolled back and the {@link SQLException} re-thrown.
 */
public final class HyosungEjTransactionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(HyosungEjTransactionRepository.class);

    private static final String INSERT_SQL =
        "INSERT INTO atm_ej_transaction_hyosung (\n" +
        "    id, file_name, file_line_start, file_line_end,\n" +
        "    sequence_no_first, sequence_no_last,\n" +
        "    card_number_raw, emv_aid, emv_app_label,\n" +
        "    amount_entered, pin_entered, cardless,\n" +
        "    opcode, function_id, amount_recorded, trans_seq_number,\n" +
        "    lt_serno, fid_response, fid_next,\n" +
        "    lt_disp_notes_1, lt_disp_notes_2, lt_disp_notes_3, lt_disp_notes_4,\n" +
        "    lt_disp_notes_5, lt_disp_notes_6, lt_disp_notes_7,\n" +
        "    denomination, request_count, pickup_count,\n" +
        "    dispense_count, remain_count, reject_count,\n" +
        "    notes_type_a, notes_type_b, notes_type_c, notes_type_d,\n" +
        "    notes_type_e, notes_type_f, notes_type_g,\n" +
        "    dispense_complete, cash_presented, cash_taken,\n" +
        "    cash_taken_after_timeout, cash_not_taken,\n" +
        "    card_ejected, card_taken,\n" +
        "    transaction_timeout, host_timeout,\n" +
        "    receipt_date, receipt_time, atm_id,\n" +
        "    receipt_card_number, receipt_account_number,\n" +
        "    txn_no, reference_no, response_code, account_type,\n" +
        "    withdrawal_amount, transaction_amount, from_account,\n" +
        "    mod_balance, avail_balance,\n" +
        "    unable_to_process, declined_insufficient, admin_subtotal,\n" +
        "    transaction_type, transaction_status,\n" +
        "    raw_transaction_block,\n" +
        "    load_batch_id, source_host\n" +
        ") VALUES (\n" +
        "    seq_atm_ej_txn_hyosung.NEXTVAL, ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?,\n" +
        "    ?, ?\n" +
        ")";

    private final DataSource ds;
    private final String     sourceHost;

    public HyosungEjTransactionRepository(DataSource ds) {
        this.ds         = ds;
        this.sourceHost = resolveHost();
    }

    /**
     * Insert all rows as a single JDBC batch within one DB transaction.
     * On success commits; on failure rolls back and re-throws.
     *
     * @return number of successfully inserted rows
     */
    public int insertBatch(Collection<EjTransaction> rows) throws SQLException {
        if (rows == null || rows.isEmpty()) return 0;

        Connection c = null;
        try {
            c = ds.getConnection();
            try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
                for (EjTransaction t : rows) {
                    bind(ps, t);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                c.commit();

                int inserted = 0;
                for (int r : results) {
                    if (r != PreparedStatement.EXECUTE_FAILED) inserted++;
                }
                LOG.info("Inserted {} Hyosung EJ transactions in batch.", inserted);
                return inserted;
            }
        } catch (SQLException e) {
            LOG.error("Batch insert failed; size={} ; cause={}", rows.size(), e.getMessage());
            safeRollback(c);
            throw e;
        } finally {
            safeClose(c);
        }
    }

    // ========================= binding =====================================
    private void bind(PreparedStatement ps, EjTransaction t) throws SQLException {
        int i = 1;

        // file identity
        setString  (ps, i++, t.getFileName());
        setLong    (ps, i++, t.getFileLineStart());
        setLong    (ps, i++, t.getFileLineEnd());

        // sequence
        setString  (ps, i++, t.getSequenceNoFirst());
        setString  (ps, i++, t.getSequenceNoLast());

        // card / EMV
        setString  (ps, i++, t.getCardNumberRaw());
        setString  (ps, i++, t.getEmvAid());
        setString  (ps, i++, t.getEmvAppLabel());

        // input
        setDecimal (ps, i++, t.getAmountEntered());
        setBoolYN  (ps, i++, t.isPinEntered());
        setBoolYN  (ps, i++, t.isCardless());

        // switch / protocol
        setString  (ps, i++, t.getOpcode());
        setString  (ps, i++, t.getFunctionId());
        setDecimal (ps, i++, t.getAmountRecorded());
        setString  (ps, i++, t.getTransSeqNumber());
        setString  (ps, i++, t.getLtSerno());
        setString  (ps, i++, t.getFidResponse());
        setString  (ps, i++, t.getFidNext());

        // LT dispense notes 1-7
        setLongObj (ps, i++, t.getLtDispNotes1());
        setLongObj (ps, i++, t.getLtDispNotes2());
        setLongObj (ps, i++, t.getLtDispNotes3());
        setLongObj (ps, i++, t.getLtDispNotes4());
        setLongObj (ps, i++, t.getLtDispNotes5());
        setLongObj (ps, i++, t.getLtDispNotes6());
        setLongObj (ps, i++, t.getLtDispNotes7());

        // dispenser telemetry
        setString  (ps, i++, t.getDenomination());
        setString  (ps, i++, t.getRequestCount());
        setString  (ps, i++, t.getPickupCount());
        setString  (ps, i++, t.getDispenseCount());
        setString  (ps, i++, t.getRemainCount());
        setString  (ps, i++, t.getRejectCount());

        // notes type A-G
        setLongObj (ps, i++, t.getNotesTypeA());
        setLongObj (ps, i++, t.getNotesTypeB());
        setLongObj (ps, i++, t.getNotesTypeC());
        setLongObj (ps, i++, t.getNotesTypeD());
        setLongObj (ps, i++, t.getNotesTypeE());
        setLongObj (ps, i++, t.getNotesTypeF());
        setLongObj (ps, i++, t.getNotesTypeG());

        // lifecycle flags
        setBoolYN  (ps, i++, t.isDispenseComplete());
        setBoolYN  (ps, i++, t.isCashPresented());
        setBoolYN  (ps, i++, t.isCashTaken());
        setBoolYN  (ps, i++, t.isCashTakenAfterTimeout());
        setBoolYN  (ps, i++, t.isCashNotTaken());
        setBoolYN  (ps, i++, t.isCardEjected());
        setBoolYN  (ps, i++, t.isCardTaken());
        setBoolYN  (ps, i++, t.isTransactionTimeout());
        setBoolYN  (ps, i++, t.isHostTimeout());

        // receipt
        setString  (ps, i++, t.getReceiptDate());
        setString  (ps, i++, t.getReceiptTime());
        setString  (ps, i++, t.getAtmId());
        setString  (ps, i++, t.getReceiptCardNumber());
        setString  (ps, i++, t.getReceiptAccountNumber());

        // transaction fields
        setString  (ps, i++, t.getTxnNo());
        setString  (ps, i++, t.getReferenceNo());
        setString  (ps, i++, t.getResponseCode());
        setString  (ps, i++, t.getAccountType());
        setDecimal (ps, i++, t.getWithdrawalAmount());
        setDecimal (ps, i++, t.getTransactionAmount());
        setString  (ps, i++, t.getFromAccount());
        setDecimal (ps, i++, t.getModBalance());
        setDecimal (ps, i++, t.getAvailBalance());

        // outcome flags
        setBoolYN  (ps, i++, t.isUnableToProcess());
        setBoolYN  (ps, i++, t.isDeclinedInsufficient());
        setBoolYN  (ps, i++, t.isAdminSubtotal());

        // classification
        setString  (ps, i++, t.getTransactionType()   != null ? t.getTransactionType().name()   : null);
        setString  (ps, i++, t.getTransactionStatus() != null ? t.getTransactionStatus().name() : null);

        // raw block
        setClob    (ps, i++, t.getRawTransactionBlock());

        // audit
        setString  (ps, i++, t.getLoadBatchId());
        setString  (ps, i++, sourceHost);
    }

    // ========================= helpers =====================================
    private static void safeRollback(Connection c) {
        if (c == null) return;
        try { c.rollback(); } catch (SQLException e) { LOG.warn("Rollback failed: {}", e.getMessage()); }
    }

    private static void safeClose(Connection c) {
        if (c == null) return;
        try { c.close(); } catch (SQLException e) { LOG.warn("Close failed: {}", e.getMessage()); }
    }

    private static void setString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else           ps.setString(idx, v);
    }

    private static void setLong(PreparedStatement ps, int idx, long v) throws SQLException {
        ps.setLong(idx, v);
    }

    private static void setLongObj(PreparedStatement ps, int idx, Long v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.NUMERIC);
        else           ps.setLong(idx, v);
    }

    private static void setDecimal(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.NUMERIC);
        else           ps.setBigDecimal(idx, v);
    }

    private static void setBoolYN(PreparedStatement ps, int idx, boolean v) throws SQLException {
        ps.setString(idx, v ? "Y" : "N");
    }

    private static void setClob(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.CLOB);
        else           ps.setCharacterStream(idx, new StringReader(v), v.length());
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            LOG.warn("Unable to resolve local hostname: {}", e.getMessage());
            return "unknown";
        }
    }
}