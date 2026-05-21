package com.bupt.tarecruit.util;

import java.nio.file.Path;

/**
 * Initializes the application's CSV data files under a data directory.
 * <p>
 * Creates each required file with the correct header row when it does not yet exist.
 * </p>
 */
public class DataBootstrapper {

    /**
     * Root directory containing all CSV data files.
     */
    private final Path dataDir;

    /**
     * Creates a bootstrapper for the given data directory.
     *
     * @param dataDir root path of the CSV data store
     */
    public DataBootstrapper(Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Ensures that all standard CSV files exist with their expected header rows.
     * Files created include TA, MO, Jobs, Applications, AccountLogs, and JobLogs.
     */
    public void initialize() {
        CsvUtil.ensureFileWithHeader(dataDir.resolve("TA.csv"),
                new String[]{"email", "password", "full_name", "phone", "major", "skills", "experience", "self_evaluation", "is_disabled", "cv_path", "ai_summary"});
        CsvUtil.ensureFileWithHeader(dataDir.resolve("MO.csv"),
                new String[]{"email", "password", "full_name", "responsible_modules", "phone", "is_disabled"});
        CsvUtil.ensureFileWithHeader(dataDir.resolve("Jobs.csv"),
                new String[]{"job_id", "job_name", "mo_id", "number_of_positions", "module_name", "requirements", "start_date", "end_date", "additional_notes", "status"});
        CsvUtil.ensureFileWithHeader(dataDir.resolve("Applications.csv"),
                new String[]{"apply_id", "ta_id", "job_id", "apply_status", "update_time"});
        CsvUtil.ensureFileWithHeader(dataDir.resolve("AccountLogs.csv"),
                new String[]{"log_id", "admin_id", "target_user_id", "target_role", "action", "previous_state", "new_state", "timestamp"});
        CsvUtil.ensureFileWithHeader(dataDir.resolve("JobLogs.csv"),
                new String[]{"log_id", "admin_id", "job_id", "action", "previous_state", "new_state", "timestamp"});
    }
}
