package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV-based implementation of the {@link AccountLogDao} interface.
 * Responsible for reading and writing account log records
 * from and to the account log CSV file.
 */
public class CsvAccountLogDao implements AccountLogDao {

    /**
     * Header row used for the account log CSV file.
     */
    private static final String[] HEADER = {
            "log_id", "admin_id", "target_user_id", "target_role",
            "action", "previous_state", "new_state", "timestamp"
    };

    /**
     * Path of the CSV file storing account log records.
     */
    private final Path filePath;

    /**
     * Creates a CSV-based account log DAO and ensures that
     * the target file exists with the required header row.
     *
     * @param filePath path of the account log CSV file
     */
    public CsvAccountLogDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    /**
     * Returns all account log records stored in the CSV file.
     *
     * @return list of all account log records
     */
    @Override
    public List<AccountLog> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<AccountLog> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    /**
     * Saves a new account log record to the CSV file.
     *
     * @param log account log entity to save
     */
    @Override
    public void save(AccountLog log) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(log));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

    /**
     * Converts a CSV row into an {@link AccountLog} entity.
     *
     * @param row CSV row data
     * @return mapped account log entity
     */
    private AccountLog mapRow(String[] row) {
        AccountLog log = new AccountLog();
        log.setLogId(rowAt(row, 0));
        log.setAdminId(rowAt(row, 1));
        log.setTargetUserId(rowAt(row, 2));
        log.setTargetRole(parseRole(rowAt(row, 3)));
        log.setAction(parseAction(rowAt(row, 4)));
        log.setPreviousState(rowAt(row, 5));
        log.setNewState(rowAt(row, 6));
        DateTimeUtil.parseDateTime(rowAt(row, 7)).ifPresent(log::setTimestamp);
        return log;
    }

    /**
     * Converts an {@link AccountLog} entity into a CSV row.
     *
     * @param log account log entity
     * @return CSV row representation of the entity
     */
    private String[] mapToRow(AccountLog log) {
        return new String[]{
                log.getLogId(),
                log.getAdminId(),
                log.getTargetUserId(),
                log.getTargetRole() == Role.TA ? "TA" : "MO",
                log.getAction().name(),
                log.getPreviousState(),
                log.getNewState(),
                DateTimeUtil.formatDateTime(log.getTimestamp())
        };
    }

    /**
     * Parses a role value from CSV text.
     *
     * @param value role text value
     * @return parsed user role
     */
    private Role parseRole(String value) {
        if ("TA".equalsIgnoreCase(value)) {
            return Role.TA;
        }
        return Role.MO;
    }

    /**
     * Parses an account action value from CSV text.
     * Returns DISABLE as a fallback when parsing fails.
     *
     * @param value action text value
     * @return parsed account action
     */
    private AccountLog.AccountAction parseAction(String value) {
        try {
            return AccountLog.AccountAction.valueOf(value);
        } catch (Exception e) {
            return AccountLog.AccountAction.DISABLE;
        }
    }

    /**
     * Safely returns the value at the specified index from a CSV row.
     * Returns an empty string if the index is out of bounds.
     *
     * @param row CSV row data
     * @param index target column index
     * @return row value at the given index, or an empty string when unavailable
     */
    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }
}