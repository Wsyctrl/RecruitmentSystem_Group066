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

public class CsvJobDao implements JobDao {

    private static final String[] HEADER = {"job_id", "job_name", "mo_id", "number_of_positions", "module_name", "requirements", "start_date", "end_date", "additional_notes", "status"};
    private final Path filePath;

    public CsvJobDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<Job> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Job> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public Optional<Job> findById(String jobId) {
        return findAll().stream().filter(job -> job.getJobId().equalsIgnoreCase(jobId)).findFirst();
    }

    @Override
    public void save(Job job) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(job));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

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
        job.setStatus(JobStatus.fromCode(parseInt(rowAt(row, 9))));
        return job;
    }

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
                String.valueOf(job.getStatus().getCode())
        };
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    private int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
