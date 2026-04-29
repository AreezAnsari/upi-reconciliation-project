package com.jpb.reconciliation.reconciliation.dto;

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
public class RoleApproveRequest {

    private Long roleId;
    private String approvedYn;   // "Y" = Approve, "N" = Disapprove
    private String roleStatus;   // "active" / "inactive"
    private String approvedBy;   // set internally from JWT — do not pass from frontend
}
