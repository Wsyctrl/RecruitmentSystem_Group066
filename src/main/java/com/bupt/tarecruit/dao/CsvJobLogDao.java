package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.JobLog;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CsvJobLogDao implements JobLogDao {

    private static final String[] HEADER = {"log_id", "admin_id", "job_id", "action", "previous_state", "new_state", "timestamp"};
    private final Path filePath;

    public CsvJobLogDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<JobLog> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<JobLog> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public void save(JobLog log) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(log));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

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

    private JobLog.JobLogAction parseAction(String value) {
        try {
            return JobLog.JobLogAction.valueOf(value);
        } catch (Exception e) {
            return JobLog.JobLogAction.CLOSE_JOB;
        }
    }

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }
}
