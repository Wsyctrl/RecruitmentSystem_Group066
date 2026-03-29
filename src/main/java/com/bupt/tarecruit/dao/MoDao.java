package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Mo;

import java.util.List;
import java.util.Optional;

public interface MoDao {
    List<Mo> findAll();

    Optional<Mo> findById(String moId);

    void save(Mo mo);

    void update(Mo mo);
}
