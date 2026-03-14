package com.jpb.reconciliation.reconciliation.service.forcematch;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForceMatchExecutionService {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Executes SP_FORCE_MATCH stored procedure.
	 *
	 * Procedure signature: SP_FORCE_MATCH ( Prm_Process_Id IN VARCHAR2, Prm_User_Id
	 * IN NUMBER, Prm_Error OUT VARCHAR2, Prm_Knockoff_Flag OUT CHAR, Prm_Ttum_Id
	 * OUT NUMBER, Prm_Ttum_Error OUT NUMBER )
	 */
	public RestWithStatusList execute(String processId, Integer userId) {
		log.info("Executing SP_FORCE_MATCH | processId={} | userId={}", processId, userId);

		SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FORCE_MATCH")
				.declareParameters(new SqlParameter("Prm_Process_Id", Types.VARCHAR),
						new SqlParameter("Prm_User_Id", Types.NUMERIC), new SqlOutParameter("Prm_Error", Types.VARCHAR),
						new SqlOutParameter("Prm_Knockoff_Flag", Types.CHAR),
						new SqlOutParameter("Prm_Ttum_Id", Types.NUMERIC),
						new SqlOutParameter("Prm_Ttum_Error", Types.NUMERIC));

		Map<String, Object> inputParams = new HashMap<>();
		inputParams.put("Prm_Process_Id", processId);
		inputParams.put("Prm_User_Id", userId);

		log.info("SP_FORCE_MATCH INPUT ::::::::::::::: {}", inputParams);

		Map<String, Object> result;
		try {
			result = simpleJdbcCall.execute(inputParams);
			log.info("SP_FORCE_MATCH RESULT ::::::::::::::: {}", result);
		} catch (Exception ex) {
			log.error("SP_FORCE_MATCH execution failed for processId={} | error={}", processId, ex.getMessage(), ex);

			Map<String, Object> errResult = new LinkedHashMap<>();
			errResult.put("processId", processId);
			errResult.put("error", ex.getMessage());
			errResult.put("ttumError", 1);

			List<Object> errData = new ArrayList<>();
			errData.add(errResult);

			return RestWithStatusList.builder().status("FAILURE").statusMsg("SP execution failed: " + ex.getMessage())
					.data(errData).build();
		}

		// ── Read OUT parameters ───────────────────────────────────────────────
		String error = (String) result.get("Prm_Error");
		String knockoffFlag = (String) result.get("Prm_Knockoff_Flag");
		Long ttumId = result.get("Prm_Ttum_Id") != null ? ((Number) result.get("Prm_Ttum_Id")).longValue() : null;
		Integer ttumError = result.get("Prm_Ttum_Error") != null ? ((Number) result.get("Prm_Ttum_Error")).intValue()
				: null;

		log.info("Prm_Error={} | Prm_Knockoff_Flag={} | Prm_Ttum_Id={} | Prm_Ttum_Error={}", error, knockoffFlag,
				ttumId, ttumError);

		// ── Build response ────────────────────────────────────────────────────
		Map<String, Object> responseData = new LinkedHashMap<>();
		responseData.put("processId", processId);
		responseData.put("error", error);
		responseData.put("knockoffFlag", knockoffFlag);
		responseData.put("ttumId", ttumId);
		responseData.put("ttumError", ttumError);

		boolean success = (ttumError == null || ttumError == 0) && error != null && error.equalsIgnoreCase("OK");

		List<Object> data = new ArrayList<>();
		data.add(responseData);

		return RestWithStatusList.builder().status(success ? "SUCCESS" : "FAILURE")
				.statusMsg(success ? "Force match executed successfully" : "Force match completed with error: " + error)
				.data(data).build();
	}
}