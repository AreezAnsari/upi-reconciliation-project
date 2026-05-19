package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecPermissionRowDTO {
	
	 // moduleId from RecModule.id — call GET /api/v1/roles/modules to get valid ids
    private Long   moduleId;
    private String moduleName;  // populated in response, ignored on request
 
    @Builder.Default private boolean hasAccess   = false;
    @Builder.Default private boolean canView     = false;
    @Builder.Default private boolean canCreate   = false;
    @Builder.Default private boolean canEdit     = false;
    @Builder.Default private boolean canApprove  = false;
    @Builder.Default private boolean canDownload = false;	
	
//    private Long    moduleId;
//    private String  moduleName;
//    private boolean hasAccess;
//    private boolean canView;
//    private boolean canCreate;
//    private boolean canEdit;
//    private boolean canApprove;
//    private boolean canDownload;
}

