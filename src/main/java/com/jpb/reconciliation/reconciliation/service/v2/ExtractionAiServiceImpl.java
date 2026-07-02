package com.jpb.reconciliation.reconciliation.service.v2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconKeyIdentifyMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.AuditLogManagerService;
import com.jpb.reconciliation.reconciliation.service.ExtractionServiceImpl;
import com.jpb.reconciliation.reconciliation.service.NTSLSettlementService;
import com.jpb.reconciliation.reconciliation.util.v2.CommonAiReport;

@Service
public class ExtractionAiServiceImpl implements ExtractionAiService {

	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;
	@Autowired ReconFieldDetailsMasterRepository reconFieldDetailsMasterRepository;
	@Autowired ReconFieldTypeMasterRepository reconFieldTypeMasterRepository;
	@Autowired ReconFieldFormatMasterRepository fieldFormatMasterRepository;
	@Autowired ReconKeyIdentifyMasterRepository reconKeyIdentifyMasterRepository;
	@Autowired SqlLoaderAiService sqlLoaderAiService;
	@Autowired CommonAiReport commonAiReport;
	@Autowired FileOpearationAiService fileOpearationAiService;
	@Autowired AuditLogManagerService auditLogManagerService;
	@Autowired ReconUserRepository reconUserRepository;
	@Autowired NTSLSettlementService ntslSettlementService;
	@Autowired ReconProcessManagerRepository processManagerRepository;
	@Autowired ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;
	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
	private final Executor extractionExecutor;
	@Value("${app.uDrive}") private String uDrivePath;
	private Logger logger = LoggerFactory.getLogger(ExtractionServiceImpl.class);

	public ExtractionAiServiceImpl(@Qualifier("extractionExecutor") Executor extractionExecutor) {
		this.extractionExecutor = extractionExecutor;
	}

	@Override
	public List<ReconBatchProcessEntity> extractionRunningStatus(List<File> processedFiles,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData) {
		List<ReconBatchProcessEntity> processList = new ArrayList<>();
		for (int i = 0; i < processedFiles.size(); i++) {
			ReconBatchProcessEntity process = new ReconBatchProcessEntity();
			process.setTemplateId(reconTemplateFileDetails.get().getTemplate().getTemplateId());
			process.setProcessType("EXTRACTION");
			process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
			process.setStatus("Running");
			process.setFileName(processedFiles.get(i).getName());
			process.setInsertUser(userData.getUserId());
			process.setInsertDate(LocalDate.now());
			process.setExtractionStatus("Running");
			reconBatchProcessEntityRepository.save(process);
			auditLogManagerService.extractionAudit(process, userData);
			processList.add(process);
		}
		return processList;
	}
  

    
	@Override
	public CompletableFuture<String> startExtraction(Optional<ReconTmpltFieldDtls> reconTemplateFileDetails,
			List<ReconBatchProcessEntity> runningExtraction, List<File> processedFiles, ReconUser userData) {
		truncateStageTable(reconTemplateFileDetails.get().getTemplate().getStageTabName());
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (int i = 0; i < runningExtraction.size(); i++) {
			int index = i;
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					String generateControlFile = generateControlFile(processedFiles.get(index),
							reconTemplateFileDetails.get().getTemplate().getDelimiter(),
							reconTemplateFileDetails.get().getTemplate().getStageTabName(),
							reconTemplateFileDetails);
					String log = generateLogFile();
					String bad = generateBadFile();
					sqlLoaderAiService.startLoading(generateControlFile, log, bad, reconTemplateFileDetails,
							runningExtraction.get(index), userData, processedFiles.get(index));
				} catch (Exception e) { logger.error("Error: ", e); }
			}, extractionExecutor);
			futures.add(future);
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> "Completed");
	}
    

    /* MY CODE START - Header-driven column mapping + Date/Number/Comma handling */
	public String generateControlFile(File fileLocation, String fileSeprator, String targetTableName, 
            Optional<ReconTmpltFieldDtls> reconTemplateFileDetails) throws IOException {

		// MY CODE START (STRUCTURAL FIX) - ROOT CAUSE OF ORA-01722 / wrong data being loaded:
		// The template's COL_POSN (e.g. 1,2,3) did NOT match the real physical position of
		// those columns in the actual CSV (e.g. Txnuid=1, Txndate=8, Txnamount=16 - the file
		// has 53 columns, template only defined 3). Because the OLD code emitted columns
		// strictly in template order with no FILLER for the skipped columns, SQL*Loader mapped
		// TXNDATE to CSV column 2 ("Uid") and TXNAMOUNT to CSV column 3 ("Adjdate") - a text
		// date string landing in a NUMBER(15,2) column, hence "ORA-01722: invalid number" on
		// every row.
		//
		// FIX: read the file's actual header row, find the REAL position of each template
		// field by matching its SHORT_NAME to the header column name, and emit a FILLER
		// column for every CSV column that isn't mapped to a template field. This makes the
		// mapping robust to template mis-configuration and to future CSV layout changes.
		Map<String, Integer> headerPositions = readHeaderPositions(fileLocation, fileSeprator);
		int maxPosition = 0;
		for (int p : headerPositions.values()) {
			if (p > maxPosition) maxPosition = p;
		}
		ReconTmpltFieldDtls[] positionFieldMap = new ReconTmpltFieldDtls[maxPosition + 1]; // 1-indexed

		for (ReconTmpltFieldDtls filed : reconTemplateFileDetails.get().getTemplate().getFieldDetails()) {
			String shortName = filed.getShortName();
			Integer pos = headerPositions.get(shortName);
			if (pos == null) {
				// fallback: case-insensitive match, in case header casing differs from SHORT_NAME
				for (Map.Entry<String, Integer> entry : headerPositions.entrySet()) {
					if (entry.getKey().equalsIgnoreCase(shortName)) {
						pos = entry.getValue();
						break;
					}
				}
			}
			if (pos != null && pos >= 1 && pos <= maxPosition) {
				positionFieldMap[pos] = filed;
			} else {
				// Field defined in template but not found in the actual file header at all.
				// Log it instead of silently mis-mapping data into the wrong column.
				logger.warn("Template field '{}' not found in header of file '{}' - this field will not be loaded",
						shortName, fileLocation.getName());
			}
		}
		// MY CODE END (STRUCTURAL FIX)

		StringBuilder content = new StringBuilder();
		content.append("OPTIONS (multithreading=TRUE, skip=1, PARALLEL=TRUE) \n");
		content.append("LOAD DATA \nINFILE '").append(fileLocation.getAbsolutePath()).append("'\n");
		content.append("INTO TABLE ").append(targetTableName).append("\nappend \n");
		content.append("FIELDS TERMINATED BY '").append(fileSeprator).append("' OPTIONALLY ENCLOSED BY '\"' \nTRAILING NULLCOLS \n(\n");

		// MY CODE START - iterate by ACTUAL FILE POSITION (1..maxPosition), not by template
		// order, so every CSV column is accounted for: either mapped to its real target field,
		// or explicitly skipped with FILLER.
		for (int pos = 1; pos <= maxPosition; pos++) {
			ReconTmpltFieldDtls filed = positionFieldMap[pos];

			if (filed == null) {
				content.append(" FILLER_COL_").append(pos).append(" FILLER,\n");
				continue;
			}

			String fieldName = filed.getShortName();
			String type = filed.getReconFieldTypeMaster().getFieldTypeDes();

			if (type.equalsIgnoreCase("DATE") || type.equalsIgnoreCase("TIMESTAMP")) {

				// Handles BOTH "13-06-2026" (dash) and "4/6/2026" (slash, single-digit day/month)
				// styles present in the same column, without relying on error-suppression syntax
				// that SQL*Loader's restricted direct-path parser does not support.
				content.append(" ").append(fieldName)
				       .append(" \"CASE ")
				       .append("WHEN INSTR(NULLIF(TRIM(BOTH '''' FROM :").append(fieldName).append("), ''), '-') > 0 ")
				       .append("THEN TO_DATE(TRIM(BOTH '''' FROM :").append(fieldName).append("), 'FMDD-FMMM-YYYY') ")
				       .append("WHEN INSTR(NULLIF(TRIM(BOTH '''' FROM :").append(fieldName).append("), ''), '/') > 0 ")
				       .append("THEN TO_DATE(TRIM(BOTH '''' FROM :").append(fieldName).append("), 'FMDD/FMMM/YYYY') ")
				       .append("ELSE NULL END\",\n");

			// MY CODE - "DECIMAL" added alongside "NUMBER": the field-type master used
			// FIELD_TYPE_DESC = 'DECIMAL' for Txnamount, which the OLD check
			// (equalsIgnoreCase("NUMBER") only) never matched, so it silently fell through to
			// the plain-CHARACTER branch (no TO_NUMBER conversion at all).
			} else if (type.equalsIgnoreCase("NUMBER") || type.equalsIgnoreCase("DECIMAL")) {

				// REGEXP_LIKE validates the cleaned value actually looks like a number (plain
				// integer/decimal or scientific notation like "6.15584E+11") before TO_NUMBER
				// runs. Blank values or genuine non-numeric text become NULL instead of
				// crashing the row with ORA-01722.
				content.append(" ").append(fieldName)
				       .append(" \"CASE WHEN REGEXP_LIKE(NULLIF(TRIM(BOTH '''' FROM REPLACE(REPLACE(:")
				       .append(fieldName).append(", ',', ''), '''', '')), ''), ")
				       .append("'^-?[0-9]*\\.?[0-9]+([eE][+-]?[0-9]+)?$') ")
				       .append("THEN TO_NUMBER(TRIM(BOTH '''' FROM REPLACE(REPLACE(:")
				       .append(fieldName).append(", ',', ''), '''', ''))) ")
				       .append("ELSE NULL END\",\n");

			} else {
				content.append(" ").append(fieldName).append(" \"TRIM(BOTH '''' FROM :").append(fieldName).append(")\",\n");
			}
		}
		// MY CODE END

		content.append(" REC_FLG CONSTANT 0,\n REV_FLAG CONSTANT N,\n FILE_NAME CONSTANT '").append(fileLocation.getName()).append("'\n)");
		String filePath = uDrivePath + "/controlfile/" + fileLocation.getName().replaceAll("\\.[^.]*$", "") + System.currentTimeMillis() + ".ctl";
		new File(filePath).getParentFile().mkdirs();
		try (FileWriter fw = new FileWriter(filePath)) { fw.write(content.toString()); }
		return filePath;
	}

	// MY CODE START - reads the file's first (header) line and returns a map of
	// columnName -> 1-based physical position, used to align template fields with their
	// real position in the actual file instead of trusting a possibly-stale COL_POSN.
	private Map<String, Integer> readHeaderPositions(File fileLocation, String fileSeprator) throws IOException {
		Map<String, Integer> headerPositions = new LinkedHashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
			String headerLine = br.readLine();
			if (headerLine != null) {
				String[] columns = headerLine.split(Pattern.quote(fileSeprator), -1);
				for (int i = 0; i < columns.length; i++) {
					String colName = columns[i].trim();
					if (colName.length() >= 2 && colName.startsWith("\"") && colName.endsWith("\"")) {
						colName = colName.substring(1, colName.length() - 1).trim();
					}
					headerPositions.put(colName, i + 1);
				}
			}
		}
		return headerPositions;
	}
	// MY CODE END

    /* MY CODE END - Header-driven column mapping + Date/Number/Comma handling */

    /*  CODE  - Utility Methods */
	private String generateBadFile() throws IOException {
		String path = uDrivePath + "/badfile/" + System.currentTimeMillis() + ".bad";
		new File(path).getParentFile().mkdirs();
		return path;
	}
	private String generateLogFile() throws IOException {
		String path = uDrivePath + "/logfile/" + System.currentTimeMillis() + ".log";
		new File(path).getParentFile().mkdirs();
		return path;
	}
    @Retryable(value = { SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 5000))
	private void truncateStageTable(String tableName) {
		jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
	}
}