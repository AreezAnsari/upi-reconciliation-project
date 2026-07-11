package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UpiAdjTtumStageItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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
    private static final String ROLE_BEN_ACQ = "BEN/ACQ";
    private static final String ROLE_REM_ISS = "REM/ISS";
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
        FLAG_MAP.put("TCC", "Transaction Credit Confirmation");
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

    @Override
    public RestWithStatusList getUpiAdjTtmStage(String adjDate) {
        try {
            log.info("Fetching ALL raw data for date: {}", adjDate);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedDate = LocalDate.parse(adjDate, fmt).format(fmt);

            String sql = "SELECT ADJTYPE, TRANSACTION_TYPE, REMITTER, BENEFICIERY, TRAN_AMOUNT FROM REC_UPI_ADJ_DATA WHERE ADJDATE = TO_DATE(?, 'DD-MM-YYYY')";
            List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(sql, formattedDate);

            List<Object> result = new ArrayList<>();

            for (Map<String, Object> row : dbRows) {
                String adjType = normalize(row.get("ADJTYPE") != null ? row.get("ADJTYPE").toString() : "");
                String txnType = normalize(row.get("TRANSACTION_TYPE") != null ? row.get("TRANSACTION_TYPE").toString() : "");
                String rem = row.get("REMITTER") != null ? row.get("REMITTER").toString() : "";
                String ben = row.get("BENEFICIERY") != null ? row.get("BENEFICIERY").toString() : "";
                double amt = row.get("TRAN_AMOUNT") != null ? ((Number) row.get("TRAN_AMOUNT")).doubleValue() : 0.0;

                String jioRole = "JIO".equals(rem) ? "REM" : ("JIO".equals(ben) ? "BEN" : "UNKNOWN");
                
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
                            if ("REM/ISS".equals(jioRole)) { dr = rule.remIssDr; cr = rule.remIssCr; }
                            else { dr = rule.benAcqDr; cr = rule.benAcqCr; }
                        }
                    }
                }

                result.add(UpiAdjTtumStageItemDto.builder()
                        .adjType(adjType)
                        .stage(adjType)
                        .flag(FLAG_MAP.getOrDefault(adjType, "–"))
                        .txn(txnType)
                        .role(financial ? jioRole : "–")
                        .fin(financial ? "Financial" : "Non-Financial")
                        .dr(nvl(dr))
                        .cr(nvl(cr))
                        .ttumRequired(financial)
                        .count(1) // Individal row hai to count 1
                        .amount(amt)
                        .build());
            }

            return RestWithStatusList.builder()
                    .status(CommonConstants.SUCCESS)
                    .statusMsg("Data fetched successfully")
                    .data(result)
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

    private String nvl(String v) { return v == null ? BLANK : v; }
}