package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rest with status list for response")
public class RestWithStatusList {

    @Schema(description = "Status of the operation", example = "SUCCESS")
    private String status;

    @Schema(description = "Message providing more information about the status", example = "Request executed successfully")
    private String statusMsg;

    @Schema(description = "List of data records")
    private List<Object> data;

    @Schema(description = "Total number of records available", example = "14000")
    private Long totalRecords;

    @Schema(description = "Total number of pages", example = "280")
    private Integer totalPages;

    @Schema(description = "Current page number (0-based)", example = "0")
    private Integer currentPage;

    // Backward compatibility constructor
    public RestWithStatusList(String status, String statusMsg, List<Object> data) {
        this.status = status;
        this.statusMsg = statusMsg;
        this.data = data;
    }
}