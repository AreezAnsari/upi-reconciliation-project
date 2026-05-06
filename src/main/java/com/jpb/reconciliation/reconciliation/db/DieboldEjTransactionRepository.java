package com.jpb.reconciliation.reconciliation.db;

import com.jpb.reconciliation.reconciliation.diebold.model.EjTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * JDBC-based repository for Diebold {@link EjTransaction}.
 * Inserts one batch into {@code atm_ej_transaction_diebold} table.
 */
public final class DieboldEjTransactionRepository {

    private static final Logger LOG =
            LoggerFactory.getLogger(DieboldEjTransactionRepository.class);

    private static final String INSERT_SQL =
        "INSERT INTO atm_ej_transaction_diebold (\n" +
        "    id, file_name, file_line_start, file_line_end,\n" +
        "    block_type_prefix, sequence_number,\n" +
        "    header_date, header_time, header_datetime,\n" +
        "    location, receipt_date, receipt_time, atm_id,\n" +
        "    card_number, txn_no, reference_no, response_code,\n" +
        "    account_type, from_account,\n" +
        "    withdrawal_amount, transaction_amount,\n" +
        "    mod_balance, avail_balance,\n" +
        "    serial_no,\n" +
        "    unable_to_process, cardless_withdrawal,\n" +
        "    processing_restrictions, card_data_error, pin_entered,\n" +
        "    error_code, error_description,\n" +
        "    transaction_type, transaction_status,\n" +
        "    raw_transaction_block,\n" +
        "    load_batch_id, source_host\n" +
        ") VALUES (\n" +
        "    seq_atm_ej_txn_diebold.NEXTVAL, ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?, ?,\n" +
        "    ?,\n" +
        "    ?, ?\n" +
        ")";

    private final DataSource ds;
    private final String     sourceHost;

    public DieboldEjTransactionRepository(DataSource ds) {
        this.ds         = ds;
        this.sourceHost = resolveHost();
    }

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
                LOG.info("Inserted {} Diebold EJ transactions in batch.", inserted);
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
        setString   (ps, i++, t.getFileName());
        setLong     (ps, i++, t.getFileLineStart());
        setLong     (ps, i++, t.getFileLineEnd());

        // header
        setString   (ps, i++, t.getBlockTypePrefix());
        setString   (ps, i++, t.getSequenceNumber());
        setString   (ps, i++, t.getHeaderDate());
        setString   (ps, i++, t.getHeaderTime());
        setTimestamp(ps, i++, t.getHeaderDateTime());

        // receipt
        setString   (ps, i++, t.getLocation());
        setString   (ps, i++, t.getReceiptDate());
        setString   (ps, i++, t.getReceiptTime());
        setString   (ps, i++, t.getAtmId());
        setString   (ps, i++, t.getCardNumber());
        setString   (ps, i++, t.getTxnNo());
        setString   (ps, i++, t.getReferenceNo());
        setString   (ps, i++, t.getResponseCode());
        setString   (ps, i++, t.getAccountType());
        setString   (ps, i++, t.getFromAccount());
        setDecimal  (ps, i++, t.getWithdrawalAmount());
        setDecimal  (ps, i++, t.getTransactionAmount());
        setDecimal  (ps, i++, t.getModBalance());
        setDecimal  (ps, i++, t.getAvailBalance());
        setString   (ps, i++, t.getSerialNo());

        // flags
        setBoolYN   (ps, i++, t.isUnableToProcess());
        setBoolYN   (ps, i++, t.isCardlessWithdrawal());
        setBoolYN   (ps, i++, t.isProcessingRestrictions());
        setBoolYN   (ps, i++, t.isCardDataError());
        setBoolYN   (ps, i++, t.isPinEntered());

        // error info
        setString   (ps, i++, t.getErrorCode());
        setString   (ps, i++, t.getErrorDescription());

        // classification
        setString   (ps, i++, t.getTransactionType()   != null ? t.getTransactionType().name()   : null);
        setString   (ps, i++, t.getTransactionStatus() != null ? t.getTransactionStatus().name() : null);

        // raw block
        setClob     (ps, i++, t.getRawTransactionBlock());

        // audit
        setString   (ps, i++, t.getLoadBatchId());
        setString   (ps, i++, sourceHost);
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

    private static void setDecimal(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.NUMERIC);
        else           ps.setBigDecimal(idx, v);
    }

    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.TIMESTAMP);
        else           ps.setTimestamp(idx, Timestamp.valueOf(v));
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
            LOG.warn("Unable to resolve hostname: {}", e.getMessage());
            return "unknown";
        }
    }
}