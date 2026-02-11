package com.jpb.reconciliation.reconciliation.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "RCN_FILE_DTL_MAST")
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "processmaster" })
public class ReconFileDetailsMaster {
	@Id
	@Column(name = "RFD_FILE_ID")
	private Long reconFileId;
	
	@Column(name = "RFD_FILE_NAME")
	private String reconFileName;
	
	@Column(name = "RFD_SHORT_NAME")
	private String reconShortName;
	
	@Column(name = "RFD_NAME_CONV_FORMAT")
	private String reconNameConvFormat;
	
	@Column(name = "RFD_FILE_DEFINE_CONST")
	private String reconFileDefinConst;
	
	@Column(name = "RFD_FILENAME_LENGTH")
	private Long reconFileNameLength;
	
	@Column(name = "RFD_FILE_DUP_CHK_FLAG")
	private String reconFileDuplicateCheckFlag;
	
	@Column(name = "RFD_FILE_TYPE")
	private String reconFileType;
	
	@Column(name = "RFD_FILE_DELIMITER")
	private String reconFileDelimiter;
	
	@Column(name = "RFD_FILE_LOCATION")
	private String reconFileLocation;
	
	@Column(name = "RFD_FILE_DEST_PATH")
	private String reconFileDestinationPath;
	
	@Column(name = "RFD_HDR_ID")
	private Long reconHdrId;
	
	@Column(name = "RFD_HDR_AVL_FLAG")
	private String reconHdrAvailableFlag;
	
	@Column(name = "RFD_HDR_BLOCK_SIZE")
	private Long reconHdrBlockSize;
	
	@Column(name = "RFD_HDR_KEY_COUNT")
	private Long reconHdrKeyCount;
	
	@Column(name = "RFD_HDR_WITH_DR")
	private String reconHdrWithDr;
	
	@Column(name = "RFD_FTR_AVAIL_FLAG")
	private String reconFtrAvailFlag;
	
	@Column(name = "RFD_FTR_BEGIN_CONST_VAL")
	private String reconFtrBeginConstVal;
	
	@Column(name = "RFD_FTR_TYPE")
	private String reconFtrType;
	
	@Column(name = "RFD_FTR_CTRL_TAG_CNT")
	private Long reconFtrControlTagCount;
	
	@Column(name = "RFD_FTR_LENGTH" )
	private Long reconFtrLength;
	
	@Column(name = "RFD_DR_FORMAT")
	private String reconDrFormat;
	
	@Column(name = "RFD_MULTI_DR_CHECK")
	private String reconMultiDrCheck;
	
	@Column(name = "RFD_MULTI_DR_COUNT")
	private Long reconMultiDrCount;
	
	@Column(name = "RFD_DR_BLOCK_SIZE_FLAG")
	private String reconDrBlockSizeFlag;
	
	@Column(name = "RFD_DR_BLOCK_SIZE")
	private Long reconDrBlockSize;
	
	@Column(name = "RFD_INST_CODE")
	private Long reconInstCode;
	
	@Column(name = "RFD_INS_USER")
	private Long reconInsertUser;
	
	@Column(name = "RFD_INS_DATE")
	private Date reconInsertDate;
	
	@Column(name = "RFD_LUPD_USER")
	private Long reconLastUpdatedUser;
	
	@Column(name = "RFD_LUPD_DATE")
	private Date reconLastUpdatedDate;
	
	@Column(name = "RFD_EXT_MENU_NAME")
	private String reconExitMenuName;
	
	@Column(name = "RFD_EXT_MENU_FLAG")
	private String reconExitMenuFlag;
	
	@Column(name = "RFD_DRIDENTIFIER_FLAG")
	private String reconDridenti1fierFlag;
	
	@Column(name = "RFD_FILE_DESCRIPTION")
	private String reconFileDescription;
	
	@Column(name = "RFD_XSD_NAME")
	private String reconXSDName;
	
	@Column(name = "RFD_DEPENDENT_FILE_ID")
	private Long reconDependentFileId;
	
	@Column(name = "RFD_FTP_SERVER_NAME")
	private String reconFTPServerName;
	
	@Column(name = "RFD_FTP_FILE_PATH")
	private String reconFTPFilePath;
	
	@Column(name = "RFD_EMAIL_SMS_FLAG")
	private String reconEmailSMSFlag;
	
	@Column(name = "RFD_SETTLE_FLG")
	private String reconSettleFlag;
	
	@Column(name = "RFD_JPSL_RPSL")
	private String reconJpslRpsl;
	
	@Column(name = "FILE_UPDATE_FLAG")
	private String fileUpdateFlag;


	@Column(name = "RFD_TRAN_FILE_FLAG")
	private String rfdTranFileFlag;
	
	@Column(name = "RFD_GL_FLAG")
	private String rfdGlFlag;

	@ManyToOne
	@JoinColumn(name = "RTD_TEMPLATE_ID", nullable = false)
	@JsonManagedReference
	private ReconTemplateDetails reconTemplateDetails;
	

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROCESS_MAST_ID")
	@JsonBackReference
	private ProcessMasterEntity processmaster;


	public Long getReconFileId() {
		return reconFileId;
	}


	public void setReconFileId(Long reconFileId) {
		this.reconFileId = reconFileId;
	}


	public String getReconFileName() {
		return reconFileName;
	}


	public void setReconFileName(String reconFileName) {
		this.reconFileName = reconFileName;
	}


	public String getReconShortName() {
		return reconShortName;
	}


	public void setReconShortName(String reconShortName) {
		this.reconShortName = reconShortName;
	}


	public String getReconNameConvFormat() {
		return reconNameConvFormat;
	}


	public void setReconNameConvFormat(String reconNameConvFormat) {
		this.reconNameConvFormat = reconNameConvFormat;
	}


	public String getReconFileDefinConst() {
		return reconFileDefinConst;
	}


	public void setReconFileDefinConst(String reconFileDefinConst) {
		this.reconFileDefinConst = reconFileDefinConst;
	}


	public Long getReconFileNameLength() {
		return reconFileNameLength;
	}


	public void setReconFileNameLength(Long reconFileNameLength) {
		this.reconFileNameLength = reconFileNameLength;
	}


	public String getReconFileDuplicateCheckFlag() {
		return reconFileDuplicateCheckFlag;
	}


	public void setReconFileDuplicateCheckFlag(String reconFileDuplicateCheckFlag) {
		this.reconFileDuplicateCheckFlag = reconFileDuplicateCheckFlag;
	}


	public String getReconFileType() {
		return reconFileType;
	}


	public void setReconFileType(String reconFileType) {
		this.reconFileType = reconFileType;
	}


	public String getReconFileDelimiter() {
		return reconFileDelimiter;
	}


	public void setReconFileDelimiter(String reconFileDelimiter) {
		this.reconFileDelimiter = reconFileDelimiter;
	}


	public String getReconFileLocation() {
		return reconFileLocation;
	}


	public void setReconFileLocation(String reconFileLocation) {
		this.reconFileLocation = reconFileLocation;
	}


	public String getReconFileDestinationPath() {
		return reconFileDestinationPath;
	}


	public void setReconFileDestinationPath(String reconFileDestinationPath) {
		this.reconFileDestinationPath = reconFileDestinationPath;
	}


	public Long getReconHdrId() {
		return reconHdrId;
	}


	public void setReconHdrId(Long reconHdrId) {
		this.reconHdrId = reconHdrId;
	}


	public String getReconHdrAvailableFlag() {
		return reconHdrAvailableFlag;
	}


	public void setReconHdrAvailableFlag(String reconHdrAvailableFlag) {
		this.reconHdrAvailableFlag = reconHdrAvailableFlag;
	}


	public Long getReconHdrBlockSize() {
		return reconHdrBlockSize;
	}


	public void setReconHdrBlockSize(Long reconHdrBlockSize) {
		this.reconHdrBlockSize = reconHdrBlockSize;
	}


	public Long getReconHdrKeyCount() {
		return reconHdrKeyCount;
	}


	public void setReconHdrKeyCount(Long reconHdrKeyCount) {
		this.reconHdrKeyCount = reconHdrKeyCount;
	}


	public String getReconHdrWithDr() {
		return reconHdrWithDr;
	}


	public void setReconHdrWithDr(String reconHdrWithDr) {
		this.reconHdrWithDr = reconHdrWithDr;
	}


	public String getReconFtrAvailFlag() {
		return reconFtrAvailFlag;
	}


	public void setReconFtrAvailFlag(String reconFtrAvailFlag) {
		this.reconFtrAvailFlag = reconFtrAvailFlag;
	}


	public String getReconFtrBeginConstVal() {
		return reconFtrBeginConstVal;
	}


	public void setReconFtrBeginConstVal(String reconFtrBeginConstVal) {
		this.reconFtrBeginConstVal = reconFtrBeginConstVal;
	}


	public String getReconFtrType() {
		return reconFtrType;
	}


	public void setReconFtrType(String reconFtrType) {
		this.reconFtrType = reconFtrType;
	}


	public Long getReconFtrControlTagCount() {
		return reconFtrControlTagCount;
	}


	public void setReconFtrControlTagCount(Long reconFtrControlTagCount) {
		this.reconFtrControlTagCount = reconFtrControlTagCount;
	}


	public Long getReconFtrLength() {
		return reconFtrLength;
	}


	public void setReconFtrLength(Long reconFtrLength) {
		this.reconFtrLength = reconFtrLength;
	}


	public String getReconDrFormat() {
		return reconDrFormat;
	}


	public void setReconDrFormat(String reconDrFormat) {
		this.reconDrFormat = reconDrFormat;
	}


	public String getReconMultiDrCheck() {
		return reconMultiDrCheck;
	}


	public void setReconMultiDrCheck(String reconMultiDrCheck) {
		this.reconMultiDrCheck = reconMultiDrCheck;
	}


	public Long getReconMultiDrCount() {
		return reconMultiDrCount;
	}


	public void setReconMultiDrCount(Long reconMultiDrCount) {
		this.reconMultiDrCount = reconMultiDrCount;
	}


	public String getReconDrBlockSizeFlag() {
		return reconDrBlockSizeFlag;
	}


	public void setReconDrBlockSizeFlag(String reconDrBlockSizeFlag) {
		this.reconDrBlockSizeFlag = reconDrBlockSizeFlag;
	}


	public Long getReconDrBlockSize() {
		return reconDrBlockSize;
	}


	public void setReconDrBlockSize(Long reconDrBlockSize) {
		this.reconDrBlockSize = reconDrBlockSize;
	}


	public Long getReconInstCode() {
		return reconInstCode;
	}


	public void setReconInstCode(Long reconInstCode) {
		this.reconInstCode = reconInstCode;
	}


	public Long getReconInsertUser() {
		return reconInsertUser;
	}


	public void setReconInsertUser(Long reconInsertUser) {
		this.reconInsertUser = reconInsertUser;
	}


	public Date getReconInsertDate() {
		return reconInsertDate;
	}


	public void setReconInsertDate(Date reconInsertDate) {
		this.reconInsertDate = reconInsertDate;
	}


	public Long getReconLastUpdatedUser() {
		return reconLastUpdatedUser;
	}


	public void setReconLastUpdatedUser(Long reconLastUpdatedUser) {
		this.reconLastUpdatedUser = reconLastUpdatedUser;
	}


	public Date getReconLastUpdatedDate() {
		return reconLastUpdatedDate;
	}


	public void setReconLastUpdatedDate(Date reconLastUpdatedDate) {
		this.reconLastUpdatedDate = reconLastUpdatedDate;
	}


	public String getReconExitMenuName() {
		return reconExitMenuName;
	}


	public void setReconExitMenuName(String reconExitMenuName) {
		this.reconExitMenuName = reconExitMenuName;
	}


	public String getReconExitMenuFlag() {
		return reconExitMenuFlag;
	}


	public void setReconExitMenuFlag(String reconExitMenuFlag) {
		this.reconExitMenuFlag = reconExitMenuFlag;
	}


	public String getReconDridenti1fierFlag() {
		return reconDridenti1fierFlag;
	}


	public void setReconDridenti1fierFlag(String reconDridenti1fierFlag) {
		this.reconDridenti1fierFlag = reconDridenti1fierFlag;
	}


	public String getReconFileDescription() {
		return reconFileDescription;
	}


	public void setReconFileDescription(String reconFileDescription) {
		this.reconFileDescription = reconFileDescription;
	}


	public String getReconXSDName() {
		return reconXSDName;
	}


	public void setReconXSDName(String reconXSDName) {
		this.reconXSDName = reconXSDName;
	}


	public Long getReconDependentFileId() {
		return reconDependentFileId;
	}


	public void setReconDependentFileId(Long reconDependentFileId) {
		this.reconDependentFileId = reconDependentFileId;
	}


	public String getReconFTPServerName() {
		return reconFTPServerName;
	}


	public void setReconFTPServerName(String reconFTPServerName) {
		this.reconFTPServerName = reconFTPServerName;
	}


	public String getReconFTPFilePath() {
		return reconFTPFilePath;
	}


	public void setReconFTPFilePath(String reconFTPFilePath) {
		this.reconFTPFilePath = reconFTPFilePath;
	}


	public String getReconEmailSMSFlag() {
		return reconEmailSMSFlag;
	}


	public void setReconEmailSMSFlag(String reconEmailSMSFlag) {
		this.reconEmailSMSFlag = reconEmailSMSFlag;
	}


	public String getReconSettleFlag() {
		return reconSettleFlag;
	}


	public void setReconSettleFlag(String reconSettleFlag) {
		this.reconSettleFlag = reconSettleFlag;
	}


	public String getReconJpslRpsl() {
		return reconJpslRpsl;
	}


	public void setReconJpslRpsl(String reconJpslRpsl) {
		this.reconJpslRpsl = reconJpslRpsl;
	}


	public String getFileUpdateFlag() {
		return fileUpdateFlag;
	}


	public void setFileUpdateFlag(String fileUpdateFlag) {
		this.fileUpdateFlag = fileUpdateFlag;
	}


	public String getRfdTranFileFlag() {
		return rfdTranFileFlag;
	}


	public void setRfdTranFileFlag(String rfdTranFileFlag) {
		this.rfdTranFileFlag = rfdTranFileFlag;
	}


	public String getRfdGlFlag() {
		return rfdGlFlag;
	}


	public void setRfdGlFlag(String rfdGlFlag) {
		this.rfdGlFlag = rfdGlFlag;
	}


	public ReconTemplateDetails getReconTemplateDetails() {
		return reconTemplateDetails;
	}


	public void setReconTemplateDetails(ReconTemplateDetails reconTemplateDetails) {
		this.reconTemplateDetails = reconTemplateDetails;
	}


	public ProcessMasterEntity getProcessmaster() {
		return processmaster;
	}


	public void setProcessmaster(ProcessMasterEntity processmaster) {
		this.processmaster = processmaster;
	}
	
	
	

}
