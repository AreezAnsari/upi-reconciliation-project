package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface Upiadjttmstageservice {

    /**
     * Returns Stage-wise TTUM Decision rows for the given ADJDATE, paginated,
     * in the exact natural (ID) order the data sits in REC_UPI_ADJ_DATA --
     * no filtering, no U2/U3-based reordering or grouping.
     *
     * @param adjDate format dd-MM-yyyy, same as the /adj-summary API
     * @param page    0-based page index
     * @param size    page size (default 50 if <= 0)
     */
    RestWithStatusList getUpiAdjTtmStage(String adjDate, int page, int size);
    byte[] downloadUpiAdjTtmStageCsv(String adjDate);   // 👈 NEW — poora data CSV banake return karega

}