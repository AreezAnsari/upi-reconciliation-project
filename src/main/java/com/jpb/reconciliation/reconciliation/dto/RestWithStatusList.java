package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Rest with status list for response")
public class RestWithStatusList {
	@Schema(description = "Status of the operation", example = "SUCCESS")
	private String status;
	@Schema(description = "Message providing more information about the status", example = "Request executed successfully")
	private String statusMsg;
	public List<Object> data;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatusMsg() {
		return statusMsg;
	}
	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}
	public List<Object> getData() {
		return data;
	}
	public void setData(List<Object> data) {
		this.data = data;
	}
	public RestWithStatusList(String status, String statusMsg, List<Object> data) {
		super();
		this.status = status;
		this.statusMsg = statusMsg;
		this.data = data;
	}
	
	
}
