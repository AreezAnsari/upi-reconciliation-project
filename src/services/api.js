import { API_CONFIG } from '../config';
import { useAuthStore } from '../store';

const apiFetch = async (url, options = {}) => {
    const response = await fetch(url, options);
    if (response.status === 401) {
        useAuthStore.getState().logout();
        throw new Error('Session expired. Please log in again.');
    }
    return response;
};

export const authAPI = {
    login: async (credentials) => {
        const response = await fetch(`${API_CONFIG.baseUrl}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                emailId: credentials.emailId,
                userName: credentials.userName,
                userPassword: credentials.userPassword,
            }),
        });
        if (!response.ok) throw new Error('Login failed');
        return response.json();
    },

    logout: async (accessToken) => {
        const response = await fetch(`${API_CONFIG.baseUrl}/auth/revoke-token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify({ refreshToken: accessToken }),
        });
        return response.ok;
    },

    getUser: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/user/getuser`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch user');
        return response.json();
    },

    getRoleLoginUser: async (roleId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/getrole-loginuser/${roleId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'text/plain',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch user role');
        return response.json();
    },

    getAllRoles: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/get-all-role`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch roles');
        return response.json();
    },

    getProcess: async (accessToken, menuFlag = 'N') => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/get-process?menuFlag=${menuFlag}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch process types');
        return response.json();
    },

    addMenu: async (menuData, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/addmenu`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(menuData),
        });
        if (!response.ok) throw new Error('Failed to add menu');
        return response.json();
    },

    getMenuByRole: async (roleId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/getMenuBy-Role/${roleId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch menu by role');
        return response.json();
    },

    startExtraction: async (processId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/extraction/start-extraction?processId=${processId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        const data = await response.json();
        if (!response.ok || data.status === 'FAILURE') {
            throw new Error(data.statusMsg || 'Failed to start extraction');
        }
        return data;
    },

    startReconciliation: async (processId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/recon/start-reconciliation?processId=${processId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to start reconciliation');
        return response.json();
    },

    refreshExtractionStatus: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/extraction/refresh-extraction-status`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to refresh extraction status');
        return response.json();
    },

    refreshReconciliationStatus: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/recon/refresh-reconciliation`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to refresh reconciliation status');
        return response.json();
    },

    generateNtslReport: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/ntsl/generate-ntsl-report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to generate report');
        return response.json();
    },

    viewExtractionDetails: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/view-extraction-details`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        const result = await response.json();
        if (!response.ok || result.status === 'FAILURE') {
            throw new Error(result.statusMsg || 'Failed to fetch extraction details');
        }
        return result;
    },

    retrieveReport: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/retrive-report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to retrieve report');
        return response.json();
    },

    getBulkforceList: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/bulkforce/get-bulkforce-list`, {
            method: 'GET',
            headers: {
                'Content-Type': 'text/plain',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch bulkforce list');
        return response.json();
    },

    processBulkforce: async (reconProcessId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/bulkforce/process-bulkforce?reconProcessId=${reconProcessId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to process bulkforce');
        return response.json();
    },

    getTtumReportData: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/ttum/get-ttum-report-data`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch TTUM report data');
        return response.json();
    },

    viewTemplates: async (page, size, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/template/view-template?page=${page}&size=${size}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch templates');
        return response.json();
    },

    createTemplate: async (templateData, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/template/template-configure`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(templateData),
        });
        if (!response.ok) throw new Error('Failed to create template');
        return response.json();
    },

    getFileConfigurations: async (page, size, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/file/file-configurations?page=${page}&size=${size}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch file configurations');
        return response.json();
    },

    getFileTemplates: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/file/templates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch file templates');
        return response.json();
    },

    createFileConfiguration: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/file/file-configurations`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to create file configuration');
        return response.json();
    },

    getProcessDefinitions: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/recon/process/view/all`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch process definitions');
        return response.json();
    },

    addProcessDefinition: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/recon/process/add`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to add process definition');
        return response.json();
    },

    updateProcessDefinition: async (processId, data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/recon/process/update/${processId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to update process definition');
        return response.json();
    },

    updateFileConfiguration: async (fileId, data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/file/file-configurations/${fileId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to update file configuration');
        return response.json();
    },

    getFieldTypes: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/field-types`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch field types');
        return response.json();
    },

    getReconFieldFormats: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/recon-field-formats`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch field formats');
        return response.json();
    },

    updateTemplate: async (templateId, templateData, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/template/update-template/${templateId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(templateData),
        });
        if (!response.ok) throw new Error('Failed to update template');
        return response.json();
    },

    generateTtumReport: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/ttum/generate-ttum-report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to generate TTUM report');
        return response.json();
    },

    // Report Config APIs
    getReportConfigs: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch report configs');
        return response.json();
    },

    createReportConfig: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to create report config');
        return response.json();
    },

    updateReportConfig: async (id, data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to update report config');
        return response.json();
    },

    deleteReportConfig: async (id, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to delete report config');
        return response.json();
    },

    getExtractionTemplates: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config/extraction/templates`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch extraction templates');
        return response.json();
    },

    getReconProcesses: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/report-config/recon/processes`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch recon processes');
        return response.json();
    },

    // User Management APIs
    getAllUsers: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/user/getallusers`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch users');
        return response.json();
    },

    getApprovedUsers: async (approvedYN, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/user/get-approved-users?approvedYN=${approvedYN}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch approved users');
        return response.json();
    },

    approveRejectUser: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/user/approve-reject-user`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to approve/reject user');
        return response.json();
    },

    createUser: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/user/create-user`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to create user');
        return response.json();
    },

    // Institution APIs
    createInstitution: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/institutions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to create institution');
        return response.json();
    },

    // Role Management APIs
    getAllMenus: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/getallmenu`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch all menus');
        return response.json();
    },

    createRole: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/role/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to create role');
        return response.json();
    },

    // FTP Server APIs
    getFtpServers: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/ftp-servers`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch FTP servers');
        return response.json();
    },

    addFtpServer: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/ftp-servers`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to add FTP server');
        return response.json();
    },

    updateFtpServer: async (id, data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/ftp-servers/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to update FTP server');
        return response.json();
    },

    deleteFtpServer: async (id, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/ftp-servers/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to delete FTP server');
        return response.json();
    },

    downloadReportZip: async (data, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/download-report-zip`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error('Failed to download report');
        return response.blob();
    },

    // ── Force Match Config ──

    getForceMatchProcessConfigs: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/process-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch force match process configs');
        return response.json();
    },

    updateForceMatchProcessConfig: async (actionId, payload, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/process-config/${actionId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error('Failed to update force match process config');
        return response.json();
    },

    createForceMatchProcessConfig: async (payload, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/process-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error('Failed to create force match process config');
        return response.json();
    },

    deleteForceMatchProcessConfig: async (processId, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/process-config/${processId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to delete force match process config');
        return response.json();
    },

    getForceMatchActionConfigs: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/action-config`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch force match action configs');
        return response.json();
    },

    updateForceMatchActionConfig: async (actionId, payload, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/action-config/${actionId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error('Failed to update force match action config');
        return response.json();
    },

    createForceMatchActionConfig: async (payload, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/action-config`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error('Failed to create force match action config');
        return response.json();
    },

    executeForceMatch: async (payload, accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/force-match/execute`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
            body: JSON.stringify(payload),
        });
        if (!response.ok) throw new Error('Failed to execute force match');
        return response.json();
    },

    getReconDashboard: async (accessToken) => {
        const response = await apiFetch(`${API_CONFIG.baseUrl}/api/v1/recon/dashboard/815065384325`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        if (!response.ok) throw new Error('Failed to fetch recon dashboard');
        return response.json();
    },
};
