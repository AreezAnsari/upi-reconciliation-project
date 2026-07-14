package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UpiAdjTtumStageItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class Upiadjttmstageserviceimpl implements Upiadjttmstageservice {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Upiadjttmstageserviceimpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final String BLANK = "";
    private static final String CUST_AC = "Cust A/C";

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\s\\u00A0\\uFEFF\\u200B]+", " ").trim();
    }

    private static class DrCrRule {
        final boolean financial;
        final String benAcqDr, benAcqCr, remIssDr, remIssCr;

        DrCrRule(boolean financial, String benAcqDr, String benAcqCr, String remIssDr, String remIssCr) {
            this.financial = financial;
            this.benAcqDr = benAcqDr;
            this.benAcqCr = benAcqCr;
            this.remIssDr = remIssDr;
            this.remIssCr = remIssCr;
        }
    }

    private static final Map<String, DrCrRule> DR_CR_MAP = new LinkedHashMap<>();
    static {
        DR_CR_MAP.put("Online Refund", new DrCrRule(true, null, null, "7067", CUST_AC));
        DR_CR_MAP.put("RET", new DrCrRule(true, null, null, "7067", CUST_AC));
        DR_CR_MAP.put("Fraud Chargeback Raise", new DrCrRule(true, "8099", "7067", "7067", "7097"));
        DR_CR_MAP.put("Fraud Chargeback Representment", new DrCrRule(true, "7067", "8099", "7097", "7067"));
        DR_CR_MAP.put("Chargeback Raise", new DrCrRule(true, "8099", "7067", "7067", "7097"));
        DR_CR_MAP.put("Chargeback Acceptance", new DrCrRule(true, CUST_AC, "8099", "7097", CUST_AC));
        DR_CR_MAP.put("Re-presentment Raise", new DrCrRule(true, "7067", "8099", "7097", "7067"));
        DR_CR_MAP.put("Wrong Credit Chargeback Raise", new DrCrRule(true, "8099", "7067", "7067", "7097"));
        DR_CR_MAP.put("Wrong credit Representment", new DrCrRule(true, "7067", "8099", "7097", "7067"));
        DR_CR_MAP.put("Credit Adjustment", new DrCrRule(true, null, null, "7067", CUST_AC));
        DR_CR_MAP.put("Remitter Generic Good Faith Credit Adjustment Acceptance", new DrCrRule(true, null, null, "7067", CUST_AC));
    }

    private static final Map<String, String> FLAG_MAP = new HashMap<>();
    static {
        FLAG_MAP.put("Online Refund", "REF");
        FLAG_MAP.put("Refund Reversal Confirmation", "RRC");
        FLAG_MAP.put("RET", "RET");
        FLAG_MAP.put("Fraud Chargeback Raise", "FC");
        FLAG_MAP.put("Fraud Chargeback Representment", "FCR");
        FLAG_MAP.put("Chargeback Raise", "B");
        FLAG_MAP.put("Chargeback Acceptance", "A");
        FLAG_MAP.put("Transaction Credit Confirmation", "TCC");
        FLAG_MAP.put("Debit Reversal Confirmation", "DRC");
        FLAG_MAP.put("Re-presentment Raise", "R");
        FLAG_MAP.put("Wrong Credit Chargeback Raise", "WC");
        FLAG_MAP.put("Wrong credit Representment", "WR");
        FLAG_MAP.put("Differed Re-presentment Raise", "FR");
        FLAG_MAP.put("Credit Adjustment", "C");
        FLAG_MAP.put("Response to Complaint", "PR2C");
        FLAG_MAP.put("Complaint Raise", "PBRB");
        FLAG_MAP.put("Pre-Arbitration Raise", "P");
        FLAG_MAP.put("Remitter Generic Good Faith Credit Adjustment Acceptance", "RGAC");
    }

    // ── Common mapping logic — used by BOTH paginated fetch AND CSV download ──
    // (extracted so both call sites stay in sync — one bug fix here fixes both)
    private UpiAdjTtumStageItemDto mapRowToDto(Map<String, Object> row) {
        String adjType = normalize(row.get("ADJTYPE") != null ? row.get("ADJTYPE").toString() : "");
        String txnType = normalize(row.get("TRANSACTION_TYPE") != null ? row.get("TRANSACTION_TYPE").toString() : "");
        String rem = normalize(row.get("REMITTER") != null ? row.get("REMITTER").toString() : "");
        String ben = normalize(row.get("BENEFICIERY") != null ? row.get("BENEFICIERY").toString() : "");
        double amt = row.get("TRAN_AMOUNT") != null ? ((Number) row.get("TRAN_AMOUNT")).doubleValue() : 0.0;

        boolean isRemIss = "JIO".equalsIgnoreCase(rem);
        boolean isBenAcq = "JIO".equalsIgnoreCase(ben);
        String roleKey = isRemIss ? "REM_ISS" : (isBenAcq ? "BEN_ACQ" : "UNKNOWN");

        boolean isU2 = "U2".equalsIgnoreCase(txnType);
        String roleLabel;
        if ("REM_ISS".equals(roleKey)) {
            roleLabel = isU2 ? "ISS" : "REM";
        } else if ("BEN_ACQ".equals(roleKey)) {
            roleLabel = isU2 ? "ACQ" : "BEN";
        } else {
            roleLabel = "UNKNOWN";
        }

        boolean financial = false;
        String dr = BLANK, cr = BLANK;

        if ("TCC".equalsIgnoreCase(adjType)) {
            financial = "U3".equalsIgnoreCase(txnType);
            if (financial) { dr = "7067"; cr = CUST_AC; }
        } else {
            DrCrRule rule = DR_CR_MAP.get(adjType);
            if (rule != null) {
                financial = rule.financial;
                if (financial) {
                    if ("REM_ISS".equals(roleKey)) {
                        dr = rule.remIssDr; cr = rule.remIssCr;
                    } else if ("BEN_ACQ".equals(roleKey)) {
                        dr = rule.benAcqDr; cr = rule.benAcqCr;
                    }
                }
            }
        }

        return UpiAdjTtumStageItemDto.builder()
                .adjType(adjType)
                .stage(adjType)
                .flag(FLAG_MAP.getOrDefault(adjType, "–"))
                .txn(txnType)
                .role(financial ? roleLabel : "–")
                .fin(financial ? "Financial" : "Non-Financial")
                .dr(nvl(dr))
                .cr(nvl(cr))
                .ttumRequired(financial)
                .count(1)
                .amount(amt)
                .build();
    }

    @Override
    public RestWithStatusList getUpiAdjTtmStage(String adjDate, int page, int size) {
        try {
            log.info("Fetching PAGINATED data for date: {}, page: {}, size: {}", adjDate, page, size);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(adjDate, fmt);
            } catch (Exception e) {
                return RestWithStatusList.builder()
                        .status(CommonConstants.FAILURE)
                        .statusMsg("Error: Invalid date format. Please use dd-MM-yyyy")
                        .data(Collections.emptyList())
                        .build();
            }
            String formattedDate = parsedDate.format(fmt);

            int safePage = Math.max(page, 0);
            int safeSize = size <= 0 ? 50 : size;   // default page size = 50 (1-50, 51-100, ...)
            int offset = safePage * safeSize;

            String countSql = "SELECT COUNT(*) FROM REC_UPI_ADJ_DATA WHERE ADJDATE = TO_DATE(?, 'DD-MM-YYYY')";
            Long totalRecords = jdbcTemplate.queryForObject(countSql, Long.class, formattedDate);
            if (totalRecords == null) totalRecords = 0L;
            int totalPages = (int) Math.ceil((double) totalRecords / safeSize);

            if (totalRecords == 0) {
                return RestWithStatusList.builder()
                        .status(CommonConstants.FAILURE)
                        .statusMsg("No adjustment data found for date: " + formattedDate)
                        .data(Collections.emptyList())
                        .totalRecords(0L)
                        .totalPages(0)
                        .currentPage(safePage)
                        .build();
            }

            // FIX -- Oracle's OFFSET/FETCH NEXT has NO guaranteed row order without an
            // ORDER BY. Without it, the DB can return rows in a different physical order
            // on each call, which can duplicate or skip rows between pages. ORDER BY TXN_UID
            // (primary key, insertion order) makes pagination stable and matches the
            // natural order the data was loaded into the table -- no artificial reordering,
            // no grouping by U2/U3 or anything else, exactly as it sits in the DB.
            String pagedSql =
                    "SELECT TXN_UID, ADJTYPE, TRANSACTION_TYPE, REMITTER, BENEFICIERY, TRAN_AMOUNT " +
                    "FROM REC_UPI_ADJ_DATA " +
                    "WHERE ADJDATE = TO_DATE(?, 'DD-MM-YYYY') " +
                    "ORDER BY TXN_UID " +
                    "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

            List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(pagedSql, formattedDate, offset, safeSize);

            List<Object> result = new ArrayList<>();
            for (Map<String, Object> row : dbRows) {
                result.add(mapRowToDto(row));
            }

            return RestWithStatusList.builder()
                    .status(CommonConstants.SUCCESS)
                    .statusMsg("Data fetched successfully")
                    .data(result)
                    .totalRecords(totalRecords)
                    .totalPages(totalPages)
                    .currentPage(safePage)
                    .build();

        } catch (Exception e) {
            log.error("Error in getUpiAdjTtmStage: {}", e.getMessage());
            return RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Error: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ── NEW — Download full (non-paginated) Stage-wise TTUM data as CSV ───────
    @Override
    public byte[] downloadUpiAdjTtmStageCsv(String adjDate) {
        try {
            log.info("Generating CSV download for date: {}", adjDate);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate parsedDate = LocalDate.parse(adjDate, fmt);
            String formattedDate = parsedDate.format(fmt);

            // Poora data ek saath — bina OFFSET/FETCH ke, sirf download ke liye
            String fullSql =
                    "SELECT TXN_UID, ADJTYPE, TRANSACTION_TYPE, REMITTER, BENEFICIERY, TRAN_AMOUNT " +
                    "FROM REC_UPI_ADJ_DATA " +
                    "WHERE ADJDATE = TO_DATE(?, 'DD-MM-YYYY') " +
                    "ORDER BY TXN_UID";

            List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(fullSql, formattedDate);

            StringBuilder csv = new StringBuilder();
            csv.append("Sr.,Dispute/Adjustment Stage,Flag,Txn Type,JIO Role,Financial,DR Account,CR Account,TTUM Required,Amount\n");

            int sr = 1;
            for (Map<String, Object> row : dbRows) {
                UpiAdjTtumStageItemDto dto = mapRowToDto(row);
                csv.append(sr++).append(",")
                   .append(csvSafe(dto.getStage())).append(",")
                   .append(csvSafe(dto.getFlag())).append(",")
                   .append(csvSafe(dto.getTxn())).append(",")
                   .append(csvSafe(dto.getRole())).append(",")
                   .append(csvSafe(dto.getFin())).append(",")
                   .append(csvSafe(dto.getDr())).append(",")
                   .append(csvSafe(dto.getCr())).append(",")
                   .append(dto.isTtumRequired() ? "Yes" : "No").append(",")
                   .append(dto.getAmount())
                   .append("\n");
            }

            log.info("CSV generated: {} rows for date {}", dbRows.size(), formattedDate);
            return csv.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error in downloadUpiAdjTtmStageCsv: {}", e.getMessage());
            return ("Error generating CSV: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    // CSV mein comma/quote/newline hone par field ko safely wrap karta hai
    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String nvl(String v) { return v == null ? BLANK : v; }
}