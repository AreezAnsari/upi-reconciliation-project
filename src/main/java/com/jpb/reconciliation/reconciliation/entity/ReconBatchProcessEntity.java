package com.jpb.reconciliation.reconciliation.entity;
import java.time.LocalDate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "rcn_batch_process")
@Data
@Entity
public class ReconBatchProcessEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
	@SequenceGenerator(name = "sequence", sequenceName = "seq_ReconBatchProcessEntity", allocationSize = 1)
	@Column(name = "RBP_SNO")
	private Long sequenceNo;
	
	@Column(name = "RBP_PROCESS_ID")
	private Long processId;
	
	@Column(name = "RBP_PROCESS_TYPE")
	private String processType;
	
	@Column(name = "RBP_START_TIME")
	private String startTime;
	
	@Column(name = "RBP_END_TIME")
	private String endTime;
	
	@Column(name = "RBP_STATUS")
	private String status;
	
	@Column(name = "RBP_FILE_NAME")
	private String fileName;
	
	@Column(name = "RBP_HEADER_DETAILS")
	private String headerDetails;
	
	@Column(name = "RBP_CTF_HEADER_DETAILS")
	private String controlFileHeaderDetails;
	
	@Column(name = "RBP_SEQ_HEADER_DETAILS")
	private Date seqHeaderDetails;
	
	@Column(name = "RBP_INST_CODE")
	private Long instCode;
	
	@Column(name = "RBP_INS_USER")
	private Long insertUser;
	
	@Column(name = "RBP_INS_DATE")
	private LocalDate insertDate;
	
	@Column(name = "RBP_EXT_STATUS")
	private String extractionStatus;
	
	@Column(name = "RBP_FILE_DATE")
	private String fileDate;

	@Column(name = "RBP_ERROR_DESC")
	private String errorDescription;
	
	@Column(name = "RBP_EXT_PROCEDURE_STATUS")
	private String extractionProcedureStatus;
	
	@Column(name = "RBP_SETL_PROCEDURE_STATUS")
	private String settleProcedureStatus;
	
	@Column(name = "RBP_DATA_COUNT")
	private String dataCount;
	
	@Column(name = "RBP_RECON_CNT1")
	private String reconDataCount;
	
	@Column(name = "RBP_RECON_STATUS")
	private String reconStatus;
	
	@Column(name = "SEGRETION_STATUS")
	private String segretionStatus;
		
	@Column(name = "REPORT_STATUS")
	private String reportStatus;

	public Long getSequenceNo() {
		return sequenceNo;
	}

	public void setSequenceNo(Long sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}

	public String getProcessType() {
		return processType;
	}

	public void setProcessType(String processType) {
		this.processType = processType;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getHeaderDetails() {
		return headerDetails;
	}

	public void setHeaderDetails(String headerDetails) {
		this.headerDetails = headerDetails;
	}

	public String getControlFileHeaderDetails() {
		return controlFileHeaderDetails;
	}

	public void setControlFileHeaderDetails(String controlFileHeaderDetails) {
		this.controlFileHeaderDetails = controlFileHeaderDetails;
	}

	public Date getSeqHeaderDetails() {
		return seqHeaderDetails;
	}

	public void setSeqHeaderDetails(Date seqHeaderDetails) {
		this.seqHeaderDetails = seqHeaderDetails;
	}

	public Long getInstCode() {
		return instCode;
	}

	public void setInstCode(Long instCode) {
		this.instCode = instCode;
	}

	public Long getInsertUser() {
		return insertUser;
	}

	public void setInsertUser(Long insertUser) {
		this.insertUser = insertUser;
	}

	public LocalDate getInsertDate() {
		return insertDate;
	}

	public void setInsertDate(LocalDate insertDate) {
		this.insertDate = insertDate;
	}

	public String getExtractionStatus() {
		return extractionStatus;
	}

	public void setExtractionStatus(String extractionStatus) {
		this.extractionStatus = extractionStatus;
	}

	public String getFileDate() {
		return fileDate;
	}

	public void setFileDate(String fileDate) {
		this.fileDate = fileDate;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public String getExtractionProcedureStatus() {
		return extractionProcedureStatus;
	}

	public void setExtractionProcedureStatus(String extractionProcedureStatus) {
		this.extractionProcedureStatus = extractionProcedureStatus;
	}

	public String getSettleProcedureStatus() {
		return settleProcedureStatus;
	}

	public void setSettleProcedureStatus(String settleProcedureStatus) {
		this.settleProcedureStatus = settleProcedureStatus;
	}

	public String getDataCount() {
		return dataCount;
	}

	public void setDataCount(String dataCount) {
		this.dataCount = dataCount;
	}
	
	public String getReconDataCount() {
		return reconDataCount;
	}

	public void setReconDataCount(String reconDataCount) {
		this.reconDataCount = reconDataCount;
	}

	public String getReconStatus() {
		return reconStatus;
	}

	public void setReconStatus(String reconStatus) {
		this.reconStatus = reconStatus;
	}

	public String getSegretionStatus() {
		return segretionStatus;
	}

	public void setSegretionStatus(String segretionStatus) {
		this.segretionStatus = segretionStatus;
	}

	public String getReportStatus() {
		return reportStatus;
	}

	public void setReportStatus(String reportStatus) {
		this.reportStatus = reportStatus;
	}
	
	
}
