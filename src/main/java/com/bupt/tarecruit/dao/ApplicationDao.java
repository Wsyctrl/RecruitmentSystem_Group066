package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.ApplicationRecord;

import java.util.List;
import java.util.Optional;

public interface ApplicationDao {
    List<ApplicationRecord> findAll();

    List<ApplicationRecord> findByTaId(String taId);

    List<ApplicationRecord> findByJobId(String jobId);

    Optional<ApplicationRecord> findById(String applyId);

    void save(ApplicationRecord record);

    void update(ApplicationRecord record);
}
