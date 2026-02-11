package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "rcn_field_dtl_mast")
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReconFieldDetailsMaster {

	@Id
	@Column(name = "RFM_COL_POSN")
	private Long reconColumnPosn;

	@Column(name = "RFM_FIELD_ID")
	private Long reconFieldId;

	@Column(name = "RFM_TEMPLATE_ID")
	private Long reconTemplateId;

	@Column(name = "RFM_SUBTEMP_ID")
	private Long reconSubTempId;

	@Column(name = "RFM_SHORT_NAME")
	private String reconShortName;

	@Column(name = "RFM_TAB_FIELD_NAME")
	private String reconTabFieldName;

	@Column(name = "RFM_FIELD_TYPE", insertable = false, updatable = false)
	private Long reconFieldType;

	@Column(name = "RFM_FIELD_FORMAT")
	private Long reconFieldFormat;

	@Column(name = "RFM_FROM_POSITION")
	private String reconFromPosition;

	@Column(name = "RFM_TO_POSITION")
	private String reconToPosition;

	@Column(name = "RFM_MAX_LENGTH")
	private Long reconMaxLength;

	@Column(name = "RFM_KEY_IDENTIFIER")
	private Long reconKeyIdentifier;

	@Column(name = "RFM_COL_OFFSET")
	private String reconColumnOffset;

	@Column(name = "RFM_MANDTORY_FLAG")
	private String reconMandtoryFlag;

	@Column(name = "RFM_INS_CODE")
	private Long reconInserCode;

	@Column(name = "RFM_INS_USER")
	private Long reconInsertUser;

	@Column(name = "RFM_INS_DATE")
	private Date reconInsertDate;

	@Column(name = "RFM_LUPD_USER")
	private Long reconLastUpdatedUser;

	@Column(name = "RFM_LUPD_DATE")
	private Date reconLastUpdatedDate;

	@Column(name = "RFM_RANK_IDENTIFIER")
	private String reconRankIdentifier;

	@Column(name = "RFM_ALTER_FLAG")
	private String reconAlterFlag;

	@Column(name = "RFM_DISPLAYFIELD_FLAG")
	private String reconDisplayedFlag;

	@Column(name = "RFM_REPORTFIELD_FLAG")
	private String reconReportFieldFlag;

	@Column(name = "RFM_MATCHINGFIELD_FLAG")
	private String reconMatchingFieldFlag;

	@Column(name = "RFM_FROM_POSN")
	private String reconFromPosn;
	
	@Column(name = "RFM_TO_POSN")
	private String reconToPosn;
	
	@Column(name = "RFM_INST_CODE")
	private Long reconInstanceCode;

	@Column(name = "RFM_MANDATORY_FLAG")
	private String reconMandatoryFlag;

	@Column(name = "RFM_COL_POSITION")
	private String reconToPositionDescription;

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "RFM_FIELD_TYPE", nullable = false)
//	@JsonManagedReference
//	private ReconFieldTypeMaster reconFieldTypeMast;

}
