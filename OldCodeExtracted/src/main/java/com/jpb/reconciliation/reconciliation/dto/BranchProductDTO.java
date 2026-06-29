package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO returned to frontend for the Privileges modal.
 *
 * Contains:
 *  - branchId: which branch's products these are
 *  - purchasedProducts: list of product names the branch has purchased
 *                       e.g. ["UPI", "NEFT", "RTGS"]
 *
 * Frontend uses this to:
 *  1. Pre-tick the purchased product nodes in the privilege tree
 *  2. Grey-out / disable non-purchased product nodes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductDTO {

    private Long   branchId;
    private String branchName;
    private List<String> purchasedProducts;  // e.g. ["UPI", "NEFT", "RTGS"]
}