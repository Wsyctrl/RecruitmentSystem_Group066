package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CSV-based implementation of the {@link JobDao} interface.
 * Responsible for reading and writing job postings
 * from and to the jobs CSV file.
 */
public class CsvJobDao implements JobDao {

    /**
     * Header row used for the jobs CSV file.
     */
    private static final String[] HEADER = {"job_id", "job_name", "mo_id", "number_of_positions", "module_name", "requirements", "start_date", "end_date", "additional_notes", "keywords", "status"};

    /**
     * Path of the CSV file storing job records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based job DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the jobs CSV file
     */
    public CsvJobDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all job records stored in the CSV file.
     *
     * @return list of all jobs
     */
    @Override
    public List<Job> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Job> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Finds a job by its unique identifier.
     *
     * @param jobId job identifier
     * @return matching job, or empty when not found
     */
    @Override
    public Optional<Job> findById(String jobId) {
        return findAll().stream().filter(job -> job.getJobId().equalsIgnoreCase(jobId)).findFirst();
    }

    /**
     * Appends a new job record to the CSV file.
     *
     * @param job job entity to save
     */
    @Override
    public void save(Job job) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(job));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Updates an existing job record in the CSV file by job ID.
     *
     * @param job job entity with updated fields
     */
    @Override
    public void update(Job job) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].equalsIgnoreCase(job.getJobId())) {
                rows.set(i, mapToRow(job));
                break;
            }
        }
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Converts a CSV row into a {@link Job} entity.
     *
     * @param row CSV row data
     * @return mapped job entity
     */
    private Job mapRow(String[] row) {
        Job job = new Job();
        job.setJobId(rowAt(row, 0));
        job.setJobName(rowAt(row, 1));
        job.setMoId(rowAt(row, 2));
        job.setNumberOfPositions(parseInt(rowAt(row, 3)));
        job.setModuleName(rowAt(row, 4));
        job.setRequirements(rowAt(row, 5));
        DateTimeUtil.parseDate(rowAt(row, 6)).ifPresent(job::setStartDate);
        DateTimeUtil.parseDate(rowAt(row, 7)).ifPresent(job::setEndDate);
        job.setAdditionalNotes(rowAt(row, 8));
        job.setKeywords(rowAt(row, 9));
        job.setStatus(JobStatus.fromCode(parseInt(rowAt(row, 10))));
        return job;
    }

    /**
     * Converts a {@link Job} entity into a CSV row.
     *
     * @param job job entity
     * @return CSV row representation of the entity
     */
    private String[] mapToRow(Job job) {
        return new String[]{
                job.getJobId(),
                emptyIfNull(job.getJobName()),
                emptyIfNull(job.getMoId()),
                String.valueOf(job.getNumberOfPositions()),
                emptyIfNull(job.getModuleName()),
                emptyIfNull(job.getRequirements()),
                DateTimeUtil.formatDate(job.getStartDate()),
                DateTimeUtil.formatDate(job.getEndDate()),
                emptyIfNull(job.getAdditionalNotes()),
                emptyIfNull(job.getKeywords()),
                String.valueOf(job.getStatus().getCode())
        };
    }

    /**
     * Parses a boolean flag from CSV text.
     *
     * @param value boolean text value
     * @return {@code true} when the value is {@code true} (case-insensitive)
     */
    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
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
     * Returns an empty string when the value is null.
     *
     * @param value text value
     * @return original value or empty string
     */
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
