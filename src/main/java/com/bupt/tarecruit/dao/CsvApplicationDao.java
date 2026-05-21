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

/**
 * CSV-based implementation of the {@link ApplicationDao} interface.
 * Responsible for reading and writing job application records
 * from and to the applications CSV file.
 */
public class CsvApplicationDao implements ApplicationDao {

    /**
     * Header row used for the applications CSV file.
     */
    private static final String[] HEADER = {"apply_id", "ta_id", "job_id", "apply_status", "apply_time", "hired_time"};

    /**
     * Path of the CSV file storing application records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based application DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the applications CSV file
     */
    public CsvApplicationDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all application records stored in the CSV file.
     *
     * @return list of all application records
     */
    @Override
    public List<ApplicationRecord> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<ApplicationRecord> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Returns applications submitted by the given teaching assistant.
     *
     * @param taId teaching assistant identifier (email)
     * @return applications for that TA (case-insensitive match)
     */
    @Override
    public List<ApplicationRecord> findByTaId(String taId) {
        return findAll().stream()
                .filter(record -> record.getTaId().equalsIgnoreCase(taId))
                .collect(Collectors.toList());
    }

    /**
     * Returns applications for the given job.
     *
     * @param jobId job identifier
     * @return applications for that job (case-insensitive match)
     */
    @Override
    public List<ApplicationRecord> findByJobId(String jobId) {
        return findAll().stream()
                .filter(record -> record.getJobId().equalsIgnoreCase(jobId))
                .collect(Collectors.toList());
    }

    /**
     * Finds an application by its unique apply identifier.
     *
     * @param applyId application identifier
     * @return matching record, or empty when not found
     */
    @Override
    public Optional<ApplicationRecord> findById(String applyId) {
        return findAll().stream().filter(record -> record.getApplyId().equalsIgnoreCase(applyId)).findFirst();
    }

    /**
     * Appends a new application record to the CSV file.
     *
     * @param record application entity to save
     */
    @Override
    public void save(ApplicationRecord record) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(record));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Updates an existing application record in the CSV file by apply ID.
     *
     * @param record application entity with updated fields
     */
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

    /**
     * Converts a CSV row into an {@link ApplicationRecord} entity.
     * Supports legacy rows that omit {@code apply_time} or use older column layouts.
     *
     * @param row CSV row data
     * @return mapped application record
     */
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

    /**
     * Converts an {@link ApplicationRecord} entity into a CSV row.
     *
     * @param record application entity
     * @return CSV row representation of the entity
     */
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

    /**
     * Parses an integer from CSV text, returning zero on failure.
     *
     * @param value numeric text value
     * @return parsed integer, or {@code 0} when blank or invalid
     */
    private int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
