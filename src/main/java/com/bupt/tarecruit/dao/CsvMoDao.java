package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.util.CsvUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CSV-based implementation of the {@link MoDao} interface.
 * Responsible for reading and writing module organizer accounts
 * from and to the MO CSV file.
 */
public class CsvMoDao implements MoDao {

    /**
     * Header row used for the MO CSV file.
     */
    private static final String[] HEADER = {"email", "password", "full_name", "responsible_modules", "phone", "is_disabled"};

    /**
     * Path of the CSV file storing MO records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based MO DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the MO CSV file
     */
    public CsvMoDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all module organizer records stored in the CSV file.
     *
     * @return list of all MO accounts
     */
    @Override
    public List<Mo> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Mo> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Finds a module organizer by email (identity).
     *
     * @param moId module organizer email
     * @return matching MO, or empty when not found
     */
    @Override
    public Optional<Mo> findById(String moId) {
        return findAll().stream().filter(mo -> mo.getEmail().equalsIgnoreCase(moId)).findFirst();
    }

    /**
     * Appends a new module organizer record to the CSV file.
     *
     * @param mo MO entity to save
     */
    @Override
    public void save(Mo mo) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(mo));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Updates an existing module organizer record in the CSV file by email.
     *
     * @param mo MO entity with updated fields
     */
    @Override
    public void update(Mo mo) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].equalsIgnoreCase(mo.getEmail())) {
                rows.set(i, mapToRow(mo));
                break;
            }
        }
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Converts a CSV row into a {@link Mo} entity.
     * Supports current and legacy column layouts (email-first and legacy {@code mo_id} schemas).
     *
     * @param row CSV row data
     * @return mapped MO entity
     */
    private Mo mapRow(String[] row) {
        Mo mo = new Mo();
        mo.setEmail(rowAt(row, 0));
        mo.setPassword(rowAt(row, 1));
        // New schema: email,password,full_name,responsible_modules,phone,is_disabled
        if (row.length >= 6 && rowAt(row, 0).contains("@")) {
            mo.setFullName(rowAt(row, 2));
            mo.setResponsibleModules(rowAt(row, 3));
            mo.setPhone(rowAt(row, 4));
            mo.setDisabled("1".equals(rowAt(row, 5)));
        } else if (row.length >= 7) {
            // Legacy schema: mo_id,password,full_name,responsible_modules,phone,email,is_disabled
            mo.setEmail(rowAt(row, 5));
            mo.setFullName(rowAt(row, 2));
            mo.setResponsibleModules(rowAt(row, 3));
            mo.setPhone(rowAt(row, 4));
            mo.setDisabled("1".equals(rowAt(row, 6)));
        } else {
            mo.setFullName("");
            mo.setResponsibleModules(rowAt(row, 2));
            mo.setPhone(rowAt(row, 3));
            mo.setEmail(rowAt(row, 4));
            mo.setDisabled("1".equals(rowAt(row, 5)));
        }
        return mo;
    }

    /**
     * Converts a {@link Mo} entity into a CSV row.
     *
     * @param mo MO entity
     * @return CSV row representation of the entity
     */
    private String[] mapToRow(Mo mo) {
        return new String[]{
                emptyIfNull(mo.getEmail()),
                emptyIfNull(mo.getPassword()),
                emptyIfNull(mo.getFullName()),
                emptyIfNull(mo.getResponsibleModules()),
                emptyIfNull(mo.getPhone()),
                mo.isDisabled() ? "1" : "0"
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
