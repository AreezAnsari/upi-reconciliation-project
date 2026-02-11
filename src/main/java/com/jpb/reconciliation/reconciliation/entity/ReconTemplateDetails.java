package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rcn_template_dtl")
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplateDetails {

	@Id
	@Column(name = "RTD_TEMPLATE_ID")
	private Long reconTemplateId;

	@Column(name = "RTD_SUB_TEMPLATE_ID")
	private Long subTemplateId;

	@Column(name = "RTD_TYPE_ID")
	private Long reconTypeId;

	@Column(name = "RTD_TEMPLATE_NAME")
	private String reconTemplateName;

	@Column(name = "RTD_STAGE_TAB_NAME")
	private String reconStageTabName;

	@Column(name = "RTD_COL_CNT")
	private Long reconColumnCnt;

	@Column(name = "RTD_EXIST_FLAG")
	private String reconExistFlag;

	@Column(name = "RTD_RVSL_IND")
	private String reconReversalInd;

	@Column(name = "RTD_DATA_REF_FLG")
	private String reconRefFlag;

	@Column(name = "RTD_ONL_REF_FLAG")
	private String reconOnlRefFlag;

	@Column(name = "RTD_ISSACQ_FLAG")
	private String reconIssacqFlag;

	@Column(name = "RTD_DATATABLE_IND")
	private String reconDataTableInd;

	@Column(name = "RTD_MAST_FLAG")
	private String reconMasterFlag;

	@Column(name = "RTD_MAST_TEMPID")
	private String reconMasterTemplateId;

	@Column(name = "RTD_INST_CODE")
	private Long InsertCode;

	@Column(name = "RTD_INS_USER")
	private Long InsertUser;

	@Column(name = "RTD_INS_DATE")
	private Date reconInsertDate;

	@Column(name = "RTD_LUPD_USER")
	private Long reconLastUpdatedUser;

	@Column(name = "RTD_LUPD_DATE")
	private Date reconLastUpdatedDate;
	
	@Column(name = "RTD_SETTL_FLAG")
	private String settlementFlag;
	
	@Column(name = "RTD_PRODUCT_TYPE")
	private String productType;
	
	@OneToMany(mappedBy = "reconTemplateDetails")
	@JsonBackReference
	private Set<ReconFileDetailsMaster> reconFileDetailsMaster;
    
}
