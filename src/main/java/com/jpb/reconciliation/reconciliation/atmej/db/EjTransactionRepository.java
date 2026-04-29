//package com.jpb.reconciliation.reconciliation.atmej.db;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.jpb.reconciliation.reconciliation.atmej.model.EjTransaction;
//
//import javax.sql.DataSource;
//import java.io.StringReader;
//import java.math.BigDecimal;
//import java.net.InetAddress;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.SQLException;
//import java.sql.Timestamp;
//import java.sql.Types;
//import java.time.LocalDateTime;
//import java.util.Collection;
//
///**
// * JDBC-based repository for {@link EjTransaction}.
// *
// * <p>The single public method {@link #insertBatch(Collection)} commits one
// * database transaction containing all rows in the batch. Callers are expected
// * to chunk inputs to a manageable size (configured via {@code db.batchSize},
// * default 500). On batch failure the entire batch is rolled back and the
// * {@link SQLException} re-thrown - the runner is responsible for retry /
// * dead-lettering.
// *
// * <p>The {@link DataSource} is expected to provide connections with
// * {@code autoCommit=false}; this is the case for the pool created by
// * {@link DataSourceFactory}.
// */
//public final class EjTransactionRepository {
//
//    private static final Logger LOG = LoggerFactory.getLogger(EjTransactionRepository.class);
//
//    /**
//     * 41 columns / 41 placeholders. Column count and bind count must always match;
//     * see {@link #bind(PreparedStatement, EjTransaction)} for the mapping.
//     */
//    private static final String INSERT_SQL = """
//            INSERT INTO atm_ej_transaction (
//                id, file_name, file_line_start, file_line_end,
//                sequence_number, log_date, log_time, transaction_datetime,
//                atm_id, receipt_date, receipt_time,
//                card_number, card_number_raw,
//                txn_no, reference_no, response_code, function_id, txn_serial_no,
//                transaction_type, transaction_status, account_type, from_account,
//                request_amount, amount_entered, withdrawal_amount, mod_balance, avail_balance,
//                opcode,
//                notes_presented, notes_taken, notes_stacked,
//                dispense_count, rejected_count, remaining_count,
//                error_severity, diagnostic_status,
//                pin_entered, card_inserted, is_admin,
//                raw_transaction_block,
//                load_batch_id, source_host
//            ) VALUES (
//                seq_atm_ej_transaction.NEXTVAL, ?, ?, ?,
//                ?, ?, ?, ?,
//                ?, ?, ?,
//                ?, ?,
//                ?, ?, ?, ?, ?,
//                ?, ?, ?, ?,
//                ?, ?, ?, ?, ?,
//                ?,
//                ?, ?, ?,
//                ?, ?, ?,
//                ?, ?,
//                ?, ?, ?,
//                ?,
//                ?, ?
//            )
//            """;
//
//    private final DataSource ds;
//    private final String     sourceHost;
//
//    public EjTransactionRepository(DataSource ds) {
//        this.ds         = ds;
//        this.sourceHost = resolveHost();
//    }
//
//    /**
//     * Insert all rows in {@code rows} as a single JDBC batch within a single
//     * database transaction. On success the transaction is committed; on any
//     * failure it is rolled back and the {@link SQLException} re-thrown.
//     *
//     * @return the number of successfully inserted rows (driver-reported).
//     */
//    public int insertBatch(Collection<EjTransaction> rows) throws SQLException {
//        if (rows == null || rows.isEmpty()) return 0;
//
//        Connection c = null;
//        try {
//            c = ds.getConnection();
//            try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
//                for (EjTransaction t : rows) {
//                    bind(ps, t);
//                    ps.addBatch();
//                }
//                int[] results = ps.executeBatch();
//                c.commit();
//
//                int inserted = 0;
//                for (int r : results) {
//                    if (r >= 0 || r == PreparedStatement.SUCCESS_NO_INFO) inserted++;
//                }
//                LOG.info("Inserted {} EJ transactions in batch.", inserted);
//                return inserted;
//            }
//        } catch (SQLException e) {
//            LOG.error("Batch insert failed; size={} ; first cause={}", rows.size(), e.getMessage());
//            safeRollback(c);
//            throw e;
//        } finally {
//            safeClose(c);
//        }
//    }
//
//    private static void safeRollback(Connection c) {
//        if (c == null) return;
//        try {
//            c.rollback();
//        } catch (SQLException re) {
//            LOG.warn("Rollback failed: {}", re.getMessage());
//        }
//    }
//
//    private static void safeClose(Connection c) {
//        if (c == null) return;
//        try {
//            c.close();
//        } catch (SQLException ce) {
//            LOG.warn("Close failed: {}", ce.getMessage());
//        }
//    }
//
//    // ============================ binding ==================================
//    private void bind(PreparedStatement ps, EjTransaction t) throws SQLException {
//        int i = 1;
//        setString  (ps, i++, t.getFileName());
//        setLong    (ps, i++, t.getFileLineStart());
//        setLong    (ps, i++, t.getFileLineEnd());
//
//        setString  (ps, i++, t.getSequenceNumber());
//        setString  (ps, i++, t.getLogDate());
//        setString  (ps, i++, t.getLogTime());
//        setTimestamp(ps, i++, t.getTransactionDateTime());
//
//        setString  (ps, i++, t.getAtmId());
//        setString  (ps, i++, t.getReceiptDate());
//        setString  (ps, i++, t.getReceiptTime());
//
//        setString  (ps, i++, t.getCardNumber());
//        setString  (ps, i++, t.getCardNumberRaw());
//
//        setString  (ps, i++, t.getTxnNo());
//        setString  (ps, i++, t.getReferenceNo());
//        setString  (ps, i++, t.getResponseCode());
//        setString  (ps, i++, t.getFunctionId());
//        setString  (ps, i++, t.getTxnSerialNo());
//
//        setString  (ps, i++, t.getTransactionType().name());
//        setString  (ps, i++, t.getTransactionStatus().name());
//        setString  (ps, i++, t.getAccountType());
//        setString  (ps, i++, t.getFromAccount());
//
//        setDecimal (ps, i++, t.getRequestAmount());
//        setDecimal (ps, i++, t.getAmountEntered());
//        setDecimal (ps, i++, t.getWithdrawalAmount());
//        setDecimal (ps, i++, t.getModBalance());
//        setDecimal (ps, i++, t.getAvailBalance());
//
//        setString  (ps, i++, t.getOpcode());
//
//        setString  (ps, i++, t.getNotesPresented());
//        setBoolYN  (ps, i++, t.isNotesTaken());
//        setBoolYN  (ps, i++, t.isNotesStacked());
//
//        setLongObj (ps, i++, t.getDispenseCount());
//        setLongObj (ps, i++, t.getRejectedCount());
//        setLongObj (ps, i++, t.getRemainingCount());
//
//        setString  (ps, i++, t.getErrorSeverity());
//        setString  (ps, i++, t.getDiagnosticStatus());
//
//        setBoolYN  (ps, i++, t.isPinEntered());
//        setBoolYN  (ps, i++, t.isCardInserted());
//        setBoolYN  (ps, i++, t.isAdmin());
//
//        setClob    (ps, i++, t.getRawTransactionBlock());
//
//        setString  (ps, i++, t.getLoadBatchId());
//        setString  (ps, i++, sourceHost);
//    }
//
//    // ============================ binding helpers ==========================
//    private static void setString(PreparedStatement ps, int idx, String v) throws SQLException {
//        if (v == null) ps.setNull(idx, Types.VARCHAR);
//        else           ps.setString(idx, v);
//    }
//
//    private static void setLong(PreparedStatement ps, int idx, long v) throws SQLException {
//        ps.setLong(idx, v);
//    }
//
//    private static void setLongObj(PreparedStatement ps, int idx, Long v) throws SQLException {
//        if (v == null) ps.setNull(idx, Types.NUMERIC);
//        else           ps.setLong(idx, v);
//    }
//
//    private static void setDecimal(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
//        if (v == null) ps.setNull(idx, Types.NUMERIC);
//        else           ps.setBigDecimal(idx, v);
//    }
//
//    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime v) throws SQLException {
//        if (v == null) ps.setNull(idx, Types.TIMESTAMP);
//        else           ps.setTimestamp(idx, Timestamp.valueOf(v));
//    }
//
//    private static void setBoolYN(PreparedStatement ps, int idx, boolean v) throws SQLException {
//        ps.setString(idx, v ? "Y" : "N");
//    }
//
//    private static void setClob(PreparedStatement ps, int idx, String v) throws SQLException {
//        if (v == null) ps.setNull(idx, Types.CLOB);
//        else           ps.setCharacterStream(idx, new StringReader(v), v.length());
//    }
//
//    private static String resolveHost() {
//        try {
//            return InetAddress.getLocalHost().getHostName();
//        } catch (Exception e) {
//            return "unknown";
//        }
//    }
//}
