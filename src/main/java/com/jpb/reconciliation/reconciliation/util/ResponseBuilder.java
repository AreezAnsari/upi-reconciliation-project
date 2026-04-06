package com.jpb.reconciliation.reconciliation.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;


/**
 * Centralises construction of RestWithMapStatusList.
 *
 * data shape:  { "key": [ { field: value, ... }, ... ] }
 *
 * Paginated APIs always return TWO keys:
 *   "templates"  (or "fileConfigs" etc.) → the data rows
 *   "pagination"                         → one-element list with page metadata
 *
 * Example response body for viewTemplate:
 * {
 *   "status":    "SUCCESS",
 *   "statusMsg": "Templates retrieved successfully.",
 *   "data": {
 *     "templates":  [ { "templateId": 1, "templateName": "CBS_TXN", ... }, ... ],
 *     "pagination": [ {
 *       "currentPage":   0,
 *       "pageSize":      10,
 *       "totalElements": 42,
 *       "totalPages":    5,
 *       "isFirst":       true,
 *       "isLast":        false,
 *       "hasNext":       true,
 *       "hasPrevious":   false
 *     }]
 *   }
 * }
 */
public final class ResponseBuilder {

    private ResponseBuilder() {}

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_ERROR   = "ERROR";

    // ── Simple success ─────────────────────────────────────────────────────

    /** Single data key, non-empty list of rows */
    public static RestWithMapStatusList ok(String msg,
                                           String key,
                                           List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put(key, rows != null ? rows : Collections.emptyList());
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    /** Pre-built multi-key data map */
    public static RestWithMapStatusList ok(String msg,
                                           Map<String, List<Map<String, Object>>> data) {
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    /** No data — confirmations (delete, activate, etc.) */
    public static RestWithMapStatusList ok(String msg) {
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, Collections.emptyMap());
    }

    // ── Paginated success ──────────────────────────────────────────────────

    /**
     * Builds a response that ALWAYS contains both the data key and the
     * "pagination" key — even when the page is empty.
     *
     * This means the frontend can always read data["pagination"][0].totalElements
     * without null-checking, regardless of whether any rows were returned.
     *
     * @param msg         status message
     * @param dataKey     e.g. "templates", "fileConfigs"
     * @param rows        converted DTO rows (may be empty list)
     * @param page        Spring Page object (provides all metadata)
     */
    public static RestWithMapStatusList okPaged(String msg,
                                                String dataKey,
                                                List<Map<String, Object>> rows,
                                                Page<?> page) {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put(dataKey,     rows != null ? rows : Collections.emptyList());
        data.put("pagination", List.of(buildPaginationMap(page)));
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    // ── Failure / Error ────────────────────────────────────────────────────

    public static RestWithMapStatusList failure(String msg) {
        return new RestWithMapStatusList(STATUS_FAILURE, msg, Collections.emptyMap());
    }

    public static RestWithMapStatusList error(String msg) {
        return new RestWithMapStatusList(STATUS_ERROR, msg, Collections.emptyMap());
    }

    /** Validation errors packed under key "errors" */
    public static RestWithMapStatusList validationFail(List<String> errors) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String e : errors) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("error", e);
            rows.add(row);
        }
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put("errors", rows);
        return new RestWithMapStatusList(STATUS_FAILURE, "Validation failed", data);
    }

    // ── DTO → Map helpers ──────────────────────────────────────────────────

    /**
     * Converts any object to Map<String, Object> via Jackson.
     * Use in service layer when converting a single DTO.
     */
    public static Map<String, Object> toMap(Object obj,
                                             com.fasterxml.jackson.databind.ObjectMapper mapper) {
        if (obj == null) return Collections.emptyMap();
        try {
            //noinspection unchecked
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", obj.toString());
            return m;
        }
    }

    /**
     * Converts a list of objects to List<Map<String, Object>>.
     * Use in service layer when converting a list of DTOs.
     */
    public static List<Map<String, Object>> toMapList(
            List<?> items,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items != null) {
            for (Object item : items) {
                result.add(toMap(item, mapper));
            }
        }
        return result;
    }

    // ── Pagination map builder ─────────────────────────────────────────────

    /**
     * Builds the pagination metadata map from a Spring Page.
     * Single source of truth — used by okPaged() above.
     * No service class should duplicate this logic.
     *
     * Keys returned:
     *   currentPage   – 0-based page number
     *   pageSize      – requested page size
     *   totalElements – total records matching the query
     *   totalPages    – total number of pages
     *   isFirst       – true if this is the first page
     *   isLast        – true if this is the last page
     *   hasNext       – true if a next page exists
     *   hasPrevious   – true if a previous page exists
     */
    public static Map<String, Object> buildPaginationMap(Page<?> p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentPage",   p.getNumber());
        m.put("pageSize",      p.getSize());
        m.put("totalElements", p.getTotalElements());
        m.put("totalPages",    p.getTotalPages());
        m.put("isFirst",       p.isFirst());
        m.put("isLast",        p.isLast());
        m.put("hasNext",       p.hasNext());
        m.put("hasPrevious",   p.hasPrevious());
        return m;
    }
}
