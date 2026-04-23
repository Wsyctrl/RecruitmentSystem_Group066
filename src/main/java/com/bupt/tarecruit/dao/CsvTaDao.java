package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.CsvUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CsvTaDao implements TaDao {

    private static final String[] HEADER = {
            "email", "password", "full_name", "phone", "major", "skills", "experience", "self_evaluation", "is_disabled", "cv_path"
    };
    private final Path filePath;

    public CsvTaDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<Ta> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<Ta> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public Optional<Ta> findById(String taId) {
        return findAll().stream().filter(ta -> ta.getEmail().equalsIgnoreCase(taId)).findFirst();
    }

    @Override
    public void save(Ta ta) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(ta));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

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

    private Ta mapRow(String[] row) {
        Ta ta = new Ta();
        ta.setEmail(rowAt(row, 0));
        ta.setPassword(rowAt(row, 1));
        // New schema: email,password,full_name,phone,major,skills,experience,self_evaluation,is_disabled,cv_path
        if (row.length >= 10 && rowAt(row, 0).contains("@")) {
            ta.setFullName(rowAt(row, 2));
            ta.setPhone(rowAt(row, 3));
            ta.setMajor(rowAt(row, 4));
            ta.setSkills(rowAt(row, 5));
            ta.setExperience(rowAt(row, 6));
            ta.setSelfEvaluation(rowAt(row, 7));
            ta.setDisabled("1".equals(rowAt(row, 8)));
            ta.setCvPath(rowAt(row, 9));
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
        }
        return ta;
    }

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
                emptyIfNull(ta.getCvPath())
        };
    }

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
