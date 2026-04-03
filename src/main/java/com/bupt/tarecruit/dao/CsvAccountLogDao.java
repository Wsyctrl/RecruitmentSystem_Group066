package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.CsvUtil;
import com.bupt.tarecruit.util.DateTimeUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CsvAccountLogDao implements AccountLogDao {

    private static final String[] HEADER = {"log_id", "admin_id", "target_user_id", "target_role", "action", "previous_state", "new_state", "timestamp"};
    private final Path filePath;

    public CsvAccountLogDao(Path filePath) {
        this.filePath = filePath;
        CsvUtil.ensureFileWithHeader(filePath, HEADER);
    }

    @Override
    public List<AccountLog> findAll() {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        List<AccountLog> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public void save(AccountLog log) {
        List<String[]> rows = CsvUtil.readDataRows(filePath);
        rows.add(mapToRow(log));
        CsvUtil.writeAll(filePath, HEADER, rows);
    }

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

    private Role parseRole(String value) {
        if ("TA".equalsIgnoreCase(value)) {
            return Role.TA;
        }
        return Role.MO;
    }

    private AccountLog.AccountAction parseAction(String value) {
        try {
            return AccountLog.AccountAction.valueOf(value);
        } catch (Exception e) {
            return AccountLog.AccountAction.DISABLE;
        }
    }

    private String rowAt(String[] row, int index) {
        return row.length > index ? row[index] : "";
    }
}
