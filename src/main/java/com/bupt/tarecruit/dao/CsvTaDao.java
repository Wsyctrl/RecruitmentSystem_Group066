package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.CsvUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CSV-based implementation of the {@link TaDao} interface.
 * Responsible for reading and writing teaching assistant accounts
 * from and to the TA CSV file.
 */
public class CsvTaDao implements TaDao {

    /**
     * Header row used for the TA CSV file.
     */
    private static final String[] HEADER = {
            "email", "password", "full_name", "phone", "major", "skills", "experience", "self_evaluation", "is_disabled", "cv_path", "ai_summary"
    };

    /**
     * Path of the CSV file storing TA records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based TA DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the TA CSV file
     */
    public CsvTaDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all teaching assistant records stored in the CSV file.
     *
     * @return list of all TA accounts
     */
    @Override
    public List<Ta> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Ta> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Finds a teaching assistant by email (identity).
     *
     * @param taId teaching assistant email
     * @return matching TA, or empty when not found
     */
    @Override
    public Optional<Ta> findById(String taId) {
        return findAll().stream().filter(ta -> ta.getEmail().equalsIgnoreCase(taId)).findFirst();
    }

    /**
     * Appends a new teaching assistant record to the CSV file.
     *
     * @param ta TA entity to save
     */
    @Override
    public void save(Ta ta) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(ta));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Updates an existing teaching assistant record in the CSV file by email.
     *
     * @param ta TA entity with updated fields
     */
    @Override
    public void update(Ta ta) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].equalsIgnoreCase(ta.getEmail())) {
                rows.set(i, mapToRow(ta));
                break;
            }
        }
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Converts a CSV row into a {@link Ta} entity.
     * Supports current and legacy column layouts (email-first and legacy {@code ta_id} schemas).
     *
     * @param row CSV row data
     * @return mapped TA entity
     */
    private Ta mapRow(String[] row) {
        Ta ta = new Ta();
        ta.setEmail(rowAt(row, 0));
        ta.setPassword(rowAt(row, 1));
        // New schema: email,password,full_name,phone,major,skills,experience,self_evaluation,is_disabled,cv_path,ai_summary
        if (row.length >= 10 && rowAt(row, 0).contains("@")) {
            ta.setFullName(rowAt(row, 2));
            ta.setPhone(rowAt(row, 3));
            ta.setMajor(rowAt(row, 4));
            ta.setSkills(rowAt(row, 5));
            ta.setExperience(rowAt(row, 6));
            ta.setSelfEvaluation(rowAt(row, 7));
            ta.setDisabled("1".equals(rowAt(row, 8)));
            ta.setCvPath(rowAt(row, 9));
            ta.setAiSummary(rowAt(row, 10));
        } else if (row.length >= 11) {
            // Legacy schema: ta_id,password,full_name,phone,email,major,skills,experience,self_evaluation,is_disabled,cv_path
            ta.setEmail(rowAt(row, 4));
            ta.setFullName(rowAt(row, 2));
            ta.setPhone(rowAt(row, 3));
            ta.setMajor(rowAt(row, 5));
            ta.setSkills(rowAt(row, 6));
            ta.setExperience(rowAt(row, 7));
            ta.setSelfEvaluation(rowAt(row, 8));
            ta.setDisabled("1".equals(rowAt(row, 9)));
            ta.setCvPath(rowAt(row, 10));
            ta.setAiSummary("");
        } else {
            // Legacy rows without full_name: phone follows password
            ta.setFullName("");
            ta.setPhone(rowAt(row, 2));
            ta.setEmail(rowAt(row, 3));
            ta.setMajor(rowAt(row, 4));
            ta.setSkills(rowAt(row, 5));
            ta.setExperience(rowAt(row, 6));
            ta.setSelfEvaluation(rowAt(row, 7));
            ta.setDisabled("1".equals(rowAt(row, 8)));
            ta.setCvPath(rowAt(row, 9));
            ta.setAiSummary("");
        }
        return ta;
    }

    /**
     * Converts a {@link Ta} entity into a CSV row.
     *
     * @param ta TA entity
     * @return CSV row representation of the entity
     */
    private String[] mapToRow(Ta ta) {
        return new String[]{
                emptyIfNull(ta.getEmail()),
                emptyIfNull(ta.getPassword()),
                emptyIfNull(ta.getFullName()),
                emptyIfNull(ta.getPhone()),
                emptyIfNull(ta.getMajor()),
                emptyIfNull(ta.getSkills()),
                emptyIfNull(ta.getExperience()),
                emptyIfNull(ta.getSelfEvaluation()),
                ta.isDisabled() ? "1" : "0",
                emptyIfNull(ta.getCvPath()),
                emptyIfNull(ta.getAiSummary())
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
     * Returns an empty string when the value is null.
     *
     * @param value text value
     * @return original value or empty string
     */
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
