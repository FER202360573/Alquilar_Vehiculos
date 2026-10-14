package com.api.alquiler.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.alquiler.models.ClienteModel;

@Repository
public interface IClienteRepository extends JpaRepository<ClienteModel, Long> {

    boolean existsByEmail(String email);

    boolean existsByLicencia(String licencia);
}
