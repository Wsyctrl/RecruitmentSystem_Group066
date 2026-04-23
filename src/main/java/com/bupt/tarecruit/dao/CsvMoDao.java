package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.util.CsvUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CsvMoDao implements MoDao {

    private static final String[] HEADER = {"email", "password", "full_name", "responsible_modules", "phone", "is_disabled"};
    private final Path filePath;

    public CsvMoDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<Mo> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Mo> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public Optional<Mo> findById(String moId) {
        return findAll().stream().filter(mo -> mo.getEmail().equalsIgnoreCase(moId)).findFirst();
    }

    @Override
    public void save(Mo mo) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(mo));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

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

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
