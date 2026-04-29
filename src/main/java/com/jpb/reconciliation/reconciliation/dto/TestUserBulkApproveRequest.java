package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TestUserBulkApproveRequest {

    // List of userIds to approve/reject in bulk
    private List<Long> userIds;

    // "Y" = Approve, "N" = Disapprove, "I" = Inactive
    private String approvedYn;

    // Optional reason — for disapproval
    private String remarks;
}