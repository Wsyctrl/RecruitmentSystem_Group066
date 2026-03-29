package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Ta;

import java.util.List;
import java.util.Optional;

public interface TaDao {
    List<Ta> findAll();

    Optional<Ta> findById(String taId);

    void save(Ta ta);

    void update(Ta ta);
}
