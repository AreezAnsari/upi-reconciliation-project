import React, { useState } from "react";
import { createSubInstitute } from "../../services/subInstituteService";

const AddSubInstitute = () => {

  const [formData, setFormData] = useState({
    name: "",
    parentInstitutionId: "",
    logoUrl: "",
    startDate: "",
    endDate: "",
    status: "ACTIVE"
  });

  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState("");
  const [loading, setLoading] = useState(false);

  // Handle input change
  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value
    }));
  };

  // Handle submit
  const handleSubmit = async (e) => {
    e.preventDefault();

    setError(null);
    setSuccessMessage("");

    // ✅ Validation
    if (new Date(formData.endDate) < new Date(formData.startDate)) {
      setError("End Date must be greater than Start Date ❌");
      return;
    }

    try {
      setLoading(true);

      // ✅ Fix payload (IMPORTANT)
      const payload = {
        ...formData,
        parentInstitutionId: Number(formData.parentInstitutionId),
        startDate: formData.startDate + ":00",
        endDate: formData.endDate + ":00"
      };

      console.log("Sending Payload:", payload);

      const response = await createSubInstitute(payload);

      console.log("Response:", response);

      setSuccessMessage("Sub-Institute created successfully ✅");

      // Reset form
      setFormData({
        name: "",
        parentInstitutionId: "",
        logoUrl: "",
        startDate: "",
        endDate: "",
        status: "ACTIVE"
      });

    } catch (err) {
      console.error("Full Error:", err?.response || err);

     setError(
  err?.response?.data?.message ||
  err?.response?.data ||
  err.message ||
  JSON.stringify(err)
);

    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mt-5">
      <div className="row justify-content-center">
        <div className="col-md-8">

          <div className="card shadow-lg border-0">

            {/* Header */}
            <div className="card-header bg-primary text-white">
              <h4 className="mb-0">Add Sub-Institute</h4>
            </div>

            <div className="card-body">

              {/* Success Message */}
              {successMessage && (
                <div className="alert alert-success">
                  {successMessage}
                </div>
              )}

              {/* Error Message */}
              {error && (
                <div className="alert alert-danger">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit}>

                <div className="row">

                  {/* Name */}
                  <div className="col-md-6 mb-3">
                    <label className="form-label">Sub-Institute Name</label>
                    <input
                      type="text"
                      className="form-control"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* Parent ID */}
                  <div className="col-md-6 mb-3">
                    <label className="form-label">Parent Institution ID</label>
                    <input
                      type="number"
                      className="form-control"
                      name="parentInstitutionId"
                      value={formData.parentInstitutionId}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* Logo */}
                  <div className="col-md-12 mb-3">
                    <label className="form-label">Logo URL</label>
                    <input
                      type="text"
                      className="form-control"
                      name="logoUrl"
                      value={formData.logoUrl}
                      onChange={handleChange}
                    />
                  </div>

                  {/* Start Date */}
                  <div className="col-md-6 mb-3">
                    <label className="form-label">Start Date</label>
                    <input
                      type="datetime-local"
                      className="form-control"
                      name="startDate"
                      value={formData.startDate}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* End Date */}
                  <div className="col-md-6 mb-3">
                    <label className="form-label">End Date</label>
                    <input
                      type="datetime-local"
                      className="form-control"
                      name="endDate"
                      value={formData.endDate}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  {/* Status */}
                  <div className="col-md-6 mb-3">
                    <label className="form-label">Status</label>
                    <select
                      className="form-select"
                      name="status"
                      value={formData.status}
                      onChange={handleChange}
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                      <option value="BLOCK">BLOCK</option>
                    </select>
                  </div>

                </div>

                {/* Submit Button */}
                <div className="text-end">
                  <button
                    type="submit"
                    className="btn btn-primary px-4"
                    disabled={loading}
                  >
                    {loading ? "Creating..." : "Create"}
                  </button>
                </div>

              </form>

            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

export default AddSubInstitute;