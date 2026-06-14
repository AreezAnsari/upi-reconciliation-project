package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecPermissionRowDTO {

    private Long   moduleId;
    private String moduleName;

    @Builder.Default private boolean hasAccess   = false;
    @Builder.Default private boolean canView     = false;
    @Builder.Default private boolean canCreate   = false;
    @Builder.Default private boolean canEdit     = false;
    @Builder.Default private boolean canApprove  = false;
    @Builder.Default private boolean canDownload = false;
}
