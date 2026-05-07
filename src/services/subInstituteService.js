import axios from "axios";

// Backend API URL
const API_URL = "http://localhost:8081/api/sub-institutes";

// Create Sub Institute API
export const createSubInstitute = async (data) => {

    try {

        console.log("Sending Data:", data);

        const response = await axios.post(API_URL, data, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        console.log("API Response:", response.data);

        return response.data;

    } catch (error) {

        console.error("FULL API ERROR:", error);

        if (error.response) {
            console.error("Response Data:", error.response.data);
            console.error("Response Status:", error.response.status);
            console.error("Response Headers:", error.response.headers);
        } else if (error.request) {
            console.error("No Response Received:", error.request);
        } else {
            console.error("Error Message:", error.message);
        }

        throw error;
    }
};