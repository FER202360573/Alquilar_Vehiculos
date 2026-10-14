package com.api.alquiler.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.alquiler.models.OficinaModel;

@Repository
public interface IOficinaRepository extends JpaRepository<OficinaModel, Long> {
}
