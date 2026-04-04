package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CsvApplicationDao implements ApplicationDao {

    private static final String[] HEADER = {"apply_id", "ta_id", "job_id", "apply_status", "apply_time", "hired_time"};
    private final Path filePath;

    public CsvApplicationDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<ApplicationRecord> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<ApplicationRecord> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public List<ApplicationRecord> findByTaId(String taId) {
        return findAll().stream()
                .filter(record -> record.getTaId().equalsIgnoreCase(taId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationRecord> findByJobId(String jobId) {
        return findAll().stream()
                .filter(record -> record.getJobId().equalsIgnoreCase(jobId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ApplicationRecord> findById(String applyId) {
        return findAll().stream().filter(record -> record.getApplyId().equalsIgnoreCase(applyId)).findFirst();
    }

    @Override
    public void save(ApplicationRecord record) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(record));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    @Override
    public void update(ApplicationRecord record) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].equalsIgnoreCase(record.getApplyId())) {
                rows.set(i, mapToRow(record));
                break;
            }
        }
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    private ApplicationRecord mapRow(String[] row) {
        ApplicationRecord record = new ApplicationRecord();
        record.setApplyId(rowAt(row, 0));
        record.setTaId(rowAt(row, 1));
        record.setJobId(rowAt(row, 2));
        record.setStatus(ApplicationStatus.fromCode(parseInt(rowAt(row, 3))));
        // For backward compatibility, if apply_time is missing, use update_time or current time
        if (row.length > 4 && !rowAt(row, 4).isBlank()) {
            DateTimeUtil.parseDateTime(rowAt(row, 4)).ifPresent(record::setApplyTime);
        }
        if (row.length > 5 && !rowAt(row, 5).isBlank()) {
            DateTimeUtil.parseDateTime(rowAt(row, 5)).ifPresent(record::setHiredTime);
        }
        return record;
    }

    private String[] mapToRow(ApplicationRecord record) {
        return new String[]{
                record.getApplyId(),
                record.getTaId(),
                record.getJobId(),
                String.valueOf(record.getStatus().getCode()),
                DateTimeUtil.formatDateTime(record.getApplyTime()),
                record.getHiredTime() != null ? DateTimeUtil.formatDateTime(record.getHiredTime()) : ""
        };
    }

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }

    private int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
