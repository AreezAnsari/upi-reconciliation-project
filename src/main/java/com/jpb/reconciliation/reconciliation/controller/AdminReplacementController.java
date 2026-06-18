package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.AdminReplacementRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.AdminReplacementService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/api/v1/replacement")
@CrossOrigin(origins = "*")
public class AdminReplacementController {

    private static final Logger logger = LoggerFactory.getLogger(AdminReplacementController.class);

    @Autowired
    private AdminReplacementService adminReplacementService;

    @Operation(summary = "Replace an INACTIVE admin or user with a new one")
    @PostMapping(value = "/replace", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> replace(
            @RequestBody AdminReplacementRequest request,
            Authentication authentication) {
        String replacedBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Replacement request: entityType={}, originalId={}, by={}",
                request.getEntityType(), request.getOriginalEntityId(), replacedBy);
        return adminReplacementService.replace(request, replacedBy);
    }
}
