package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Rest with status list for response")
public class RestWithStatusList {
	@Schema(description = "Status of the operation", example = "SUCCESS")
	private String status;
	@Schema(description = "Message providing more information about the status", example = "Request executed successfully")
	private String statusMsg;
	public List<Object> data;
	
}
