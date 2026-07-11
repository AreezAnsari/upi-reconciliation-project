package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface Upiadjttmstageservice  {

    /**
     * Returns Stage-wise TTUM Decision rows for the given ADJDATE,
     * with DR/CR account codes resolved from REC_UPI_ADJ_DATA
     * (REMITTER / BENEFICIERY columns tell us Jio's actual role,
     * TRANSACTION_TYPE tells us U2 vs U3).
     *
     * @param adjDate format dd-MM-yyyy, same as the /adj-summary API
     */
    RestWithStatusList getUpiAdjTtmStage(String adjDate);
}