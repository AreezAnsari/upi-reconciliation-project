package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReconMenuMasterDto {

	private Long menuId;
	private String menuType;
	private String menuName;
	private String menuDescription;
	private String parentMenuCode;
	private String masterMenuParent;
	private String subMenuReq;
	private String menuUrl;
	private String operations;
	private Long userId;
	private Long roleId;
	private Long menuProcessId;
	private String processType; 
	private String reconFilePath;
}
