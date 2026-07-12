package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UpiAdjItemDto;
import com.jpb.reconciliation.reconciliation.dto.UpiAdjGroupDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class UpiAdjSummaryServiceImpl implements UpiAdjSummaryService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UpiAdjSummaryServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final Map<String, String> CATEGORY_MAP = new LinkedHashMap<>();
    static {
        CATEGORY_MAP.put("Online Refund",                  "Refund");
        CATEGORY_MAP.put("Refund Reversal Confirmation",   "Refund");
        CATEGORY_MAP.put("RET",                            "Refund");
        CATEGORY_MAP.put("Fraud Chargeback Raise",         "Chargeback");
        CATEGORY_MAP.put("Fraud Chargeback Representment", "Chargeback");
        CATEGORY_MAP.put("Chargeback Raise",               "Chargeback");
        CATEGORY_MAP.put("Chargeback Acceptance",          "Chargeback");
        CATEGORY_MAP.put("TCC",                            "True Credit/Debit");
        CATEGORY_MAP.put("Debit Reversal Confirmation",    "True Credit/Debit");
        CATEGORY_MAP.put("Re-presentment Raise",           "Re-presentment");
        CATEGORY_MAP.put("Wrong Credit Chargeback Raise",  "Re-presentment");
        CATEGORY_MAP.put("Wrong credit Representment",     "Re-presentment");
        CATEGORY_MAP.put("Differed Re-presentment Raise",  "Re-presentment");
        CATEGORY_MAP.put("Credit Adjustment",              "GL Adjustment");
        CATEGORY_MAP.put("Response to Complaint",          "GL Adjustment");
        CATEGORY_MAP.put("Complaint Raise",                "GL Adjustment");
        CATEGORY_MAP.put("Pre-Arbitration Raise",          "GL Adjustment");
    }

    private static final Map<String, String> FLAG_MAP = new LinkedHashMap<>();
    static {
        FLAG_MAP.put("Online Refund",                  "REF");
        FLAG_MAP.put("Refund Reversal Confirmation",   "RRC");
        FLAG_MAP.put("RET",                            "RET");
        FLAG_MAP.put("Fraud Chargeback Raise",         "FC");
        FLAG_MAP.put("Fraud Chargeback Representment", "FCR");
        FLAG_MAP.put("Chargeback Raise",               "CB");
        FLAG_MAP.put("Chargeback Acceptance",          "CBA");
        FLAG_MAP.put("TCC",                            "TCC");
        FLAG_MAP.put("Debit Reversal Confirmation",    "DRC");
        FLAG_MAP.put("Re-presentment Raise",           "R");
        FLAG_MAP.put("Wrong Credit Chargeback Raise",  "WR");
        FLAG_MAP.put("Wrong credit Representment",     "WCR");
        FLAG_MAP.put("Differed Re-presentment Raise",  "DRP");
        FLAG_MAP.put("Credit Adjustment",              "C");
        FLAG_MAP.put("Response to Complaint",          "RC");
        FLAG_MAP.put("Complaint Raise",                "CL");
        FLAG_MAP.put("Pre-Arbitration Raise",          "PA");
    }

    private static final Map<String, String> TTUM_MAP = new LinkedHashMap<>();
    static {
        TTUM_MAP.put("00", "Generated");
        TTUM_MAP.put("RB", "Pending");
        TTUM_MAP.put("R9", "Pending");
        TTUM_MAP.put("RR", "Approved");
        TTUM_MAP.put("U9", "Not Gen.");
        TTUM_MAP.put("UR", "Not Gen.");
    }

    private static final List<String> CATEGORY_ORDER =
            Arrays.asList("Refund", "Chargeback", "True Credit/Debit", "Re-presentment", "GL Adjustment");

    // ── Main SQL — adjtype, count, amount, response ─────────────────────────
    private static final String SQL_MAIN =
            "SELECT ADJTYPE, " +
            "       COUNT(*)          AS CNT, " +
            "       SUM(TRAN_AMOUNT)  AS TOTAL_AMT, " +
            "       MAX(RESPONSE)     AS RESPONSE " +
            "FROM   REC_UPI_ADJ_DATA " +
            "WHERE  ADJDATE = TO_DATE(?, 'DD-MM-YYYY') " +
            "GROUP  BY ADJTYPE " +
            "ORDER  BY ADJTYPE";

    // ── REM JIO Count SQL — REMITTER = JIO wale records ─────────────────────
    private static final String SQL_REM_JIO =
            "SELECT ADJTYPE, COUNT(*) AS JIO_CNT " +
            "FROM   REC_UPI_ADJ_DATA " +
            "WHERE  ADJDATE   = TO_DATE(?, 'DD-MM-YYYY') " +
            "AND    REMITTER  = 'JIO' " +
            "GROUP  BY ADJTYPE";

    // ── BEN JIO Count SQL — BENEFICIERY = JIO wale records ──────────────────
    private static final String SQL_BEN_JIO =
            "SELECT ADJTYPE, COUNT(*) AS JIO_CNT " +
            "FROM   REC_UPI_ADJ_DATA " +
            "WHERE  ADJDATE     = TO_DATE(?, 'DD-MM-YYYY') " +
            "AND    BENEFICIERY = 'JIO' " +
            "GROUP  BY ADJTYPE";

    @Override
    public ResponseEntity<RestWithStatusList> getAdjSummaryByType(String ADJ_DATE) {
        try {
            log.info("Backend received ADJ_DATE: '{}'", ADJ_DATE);
            log.info("Fetching UPI Adj Summary from REC_UPI_ADJ_DATA for date: {}", ADJ_DATE);

            // Step 0: Validate incoming date format (dd-MM-yyyy)
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate date;

            try {
                date = LocalDate.parse(ADJ_DATE, inputFormatter);
            } catch (Exception e) {
                log.error("Invalid date format received: {}", ADJ_DATE);
                return ResponseEntity.ok(RestWithStatusList.builder()
                        .status(CommonConstants.FAILURE)
                        .statusMsg("Error: Invalid date format. Please use dd-MM-yyyy")
                        .data(Collections.emptyList())
                        .build());
            }

            String formattedDate = date.format(inputFormatter);
            log.info("Formatted Date for DB: {}", formattedDate);

            // Step 1: Main data fetch
            List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(SQL_MAIN, formattedDate);
            log.info("DB rows fetched: {}", dbRows.size());

            if (dbRows.isEmpty()) {
                return ResponseEntity.ok(
                    RestWithStatusList.builder()
                        .status(CommonConstants.FAILURE)
                        .statusMsg("No adjustment data found for date: " + formattedDate)
                        .data(Collections.emptyList())
                        .build()
                );
            }

            // Step 2: REM JIO counts fetch — Map<ADJTYPE, count>
            List<Map<String, Object>> remJioRows = jdbcTemplate.queryForList(SQL_REM_JIO, formattedDate);
            Map<String, Integer> remJioMap = new HashMap<>();
            for (Map<String, Object> row : remJioRows) {
                String adjtype = row.get("ADJTYPE") != null ? row.get("ADJTYPE").toString().trim() : "";
                int    cnt     = row.get("JIO_CNT") != null ? ((Number) row.get("JIO_CNT")).intValue() : 0;
                remJioMap.put(adjtype, cnt);
            }
            log.info("REM JIO rows fetched: {}", remJioRows.size());

            // Step 3: BEN JIO counts fetch — Map<ADJTYPE, count>
            List<Map<String, Object>> benJioRows = jdbcTemplate.queryForList(SQL_BEN_JIO, formattedDate);
            Map<String, Integer> benJioMap = new HashMap<>();
            for (Map<String, Object> row : benJioRows) {
                String adjtype = row.get("ADJTYPE") != null ? row.get("ADJTYPE").toString().trim() : "";
                int    cnt     = row.get("JIO_CNT") != null ? ((Number) row.get("JIO_CNT")).intValue() : 0;
                benJioMap.put(adjtype, cnt);
            }
            log.info("BEN JIO rows fetched: {}", benJioRows.size());

            // Step 4: Build items with JIO counts
            Map<String, List<UpiAdjItemDto>> itemsMap  = new LinkedHashMap<>();
            Map<String, Integer>             countMap  = new LinkedHashMap<>();
            Map<String, Double>              amountMap = new LinkedHashMap<>();

            for (Map<String, Object> row : dbRows) {
                String adjtype  = row.get("ADJTYPE")   != null ? row.get("ADJTYPE").toString().trim()           : "";
                int    cnt      = row.get("CNT")        != null ? ((Number) row.get("CNT")).intValue()           : 0;
                double amt      = row.get("TOTAL_AMT")  != null ? ((Number) row.get("TOTAL_AMT")).doubleValue()  : 0.0;
                String response = row.get("RESPONSE")   != null ? row.get("RESPONSE").toString().trim()          : "";

                if (!CATEGORY_MAP.containsKey(adjtype)) {
                    log.warn("Unknown ADJTYPE skipped: {}", adjtype);
                    continue;
                }

                String category   = CATEGORY_MAP.get(adjtype);
                String flag       = FLAG_MAP.getOrDefault(adjtype, "??");
                String ttumStatus = TTUM_MAP.getOrDefault(response, "Unknown");

                // JIO counts — 0 if not found
                int remJio = remJioMap.getOrDefault(adjtype, 0);
                int benJio = benJioMap.getOrDefault(adjtype, 0);

                UpiAdjItemDto item = new UpiAdjItemDto();
                item.setCategory(category);
                item.setFlag(flag);
                item.setAdjustmentType(adjtype);
                item.setCount(cnt);
                item.setAmount(Math.round(amt * 100.0) / 100.0);
                item.setTtumStatus(ttumStatus);
                item.setRemJioCount(remJio);  // ← NEW
                item.setBenJioCount(benJio);  // ← NEW

                itemsMap.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
                countMap.merge(category,  cnt, Integer::sum);
                amountMap.merge(category, amt, Double::sum);
            }

            // Step 5: Build final grouped response
            List<Object> responseList  = new ArrayList<>();
            int    grandTotalRecords   = 0;
            double grandTotalAmount    = 0.0;

            for (String cat : CATEGORY_ORDER) {
                if (!itemsMap.containsKey(cat)) continue;

                int    totalRec = countMap.getOrDefault(cat, 0);
                double totalAmt = amountMap.getOrDefault(cat, 0.0);

                UpiAdjGroupDto group = new UpiAdjGroupDto();
                group.setCategory(cat);
                group.setTotalRecords(totalRec);
                group.setTotalAmount(Math.round(totalAmt * 100.0) / 100.0);
                group.setItems(itemsMap.get(cat));

                responseList.add(group);
                grandTotalRecords += totalRec;
                grandTotalAmount  += totalAmt;
            }

            Map<String, Object> grandTotal = new LinkedHashMap<>();
            grandTotal.put("grandTotalRecords", grandTotalRecords);
            grandTotal.put("grandTotalAmount",  Math.round(grandTotalAmount * 100.0) / 100.0);
            responseList.add(grandTotal);

            return ResponseEntity.ok(
                RestWithStatusList.builder()
                    .status(CommonConstants.SUCCESS)
                    .statusMsg("Request executed successfully")
                    .data(responseList)
                    .build()
            );

        } catch (Exception e) {
            log.error("Error in getAdjSummaryByType: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Error: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build());
        }
    }
}