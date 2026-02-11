package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "rcn_field_type_mast")
public class ReconFieldTypeMaster {
	
	@Id
	@Column(name = "RFT_FIELD_TYPE_ID")
	private Long fieldTypeId;
	
	@Column(name = "RFT_FIELD_TYPE_DESC")
	private String fieldTypeDes;
	
	@Column(name = "RFT_INS_USER")
	private Long insertUser;
	
	@Column(name = "RFT_INS_DATE")
	private Date insertDate;
	
	@Column(name = "RFT_LUPD_USER")
	private Long lastUpdatedUser;
	
	@Column(name = "RFT_LUPD_DATE")
	private Date lastUpdatedDate;
	
	@Column(name = "RFT_INST_CODE")
	private Long insertCode;
//	
//	@OneToMany(mappedBy = "reconFieldTypeMast")
//	@JsonBackReference
//	private Set<ReconFieldDetailsMaster> reconFieldDetailsMaster;
//	
}
