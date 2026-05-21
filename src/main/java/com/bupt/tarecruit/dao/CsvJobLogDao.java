package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.JobLog;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV-based implementation of the {@link JobLogDao} interface.
 * Responsible for reading and writing job audit log records
 * from and to the job log CSV file.
 */
public class CsvJobLogDao implements JobLogDao {

    /**
     * Header row used for the job log CSV file.
     */
    private static final String[] HEADER = {"log_id", "admin_id", "job_id", "action", "previous_state", "new_state", "timestamp"};

    /**
     * Path of the CSV file storing job log records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based job log DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the job log CSV file
     */
    public CsvJobLogDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all job log records stored in the CSV file.
     *
     * @return list of all job log records
     */
    @Override
    public List<JobLog> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<JobLog> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Saves a new job log record to the CSV file.
     *
     * @param log job log entity to save
     */
    @Override
    public void save(JobLog log) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(log));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Converts a CSV row into a {@link JobLog} entity.
     *
     * @param row CSV row data
     * @return mapped job log entity
     */
    private JobLog mapRow(String[] row) {
        JobLog log = new JobLog();
        log.setLogId(rowAt(row, 0));
        log.setAdminId(rowAt(row, 1));
        log.setJobId(rowAt(row, 2));
        log.setAction(parseAction(rowAt(row, 3)));
        log.setPreviousState(rowAt(row, 4));
        log.setNewState(rowAt(row, 5));
        DateTimeUtil.parseDateTime(rowAt(row, 6)).ifPresent(log::setTimestamp);
        return log;
    }

    /**
     * Converts a {@link JobLog} entity into a CSV row.
     *
     * @param log job log entity
     * @return CSV row representation of the entity
     */
    private String[] mapToRow(JobLog log) {
        return new String[]{
                log.getLogId(),
                log.getAdminId(),
                log.getJobId(),
                log.getAction().name(),
                log.getPreviousState(),
                log.getNewState(),
                DateTimeUtil.formatDateTime(log.getTimestamp())
        };
    }

    /**
     * Parses a job log action value from CSV text.
     * Returns {@link JobLog.JobLogAction#CLOSE_JOB} as a fallback when parsing fails.
     *
     * @param value action text value
     * @return parsed job log action
     */
    private JobLog.JobLogAction parseAction(String value) {
        try {
            return JobLog.JobLogAction.valueOf(value);
        } catch (Exception e) {
            return JobLog.JobLogAction.CLOSE_JOB;
        }
    }

    /**
     * Safely returns the value at the specified index from a CSV row.
     * Returns an empty string if the index is out of bounds.
     *
     * @param row   CSV row data
     * @param index target column index
     * @return row value at the given index, or an empty string when unavailable
     */
    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }
}
