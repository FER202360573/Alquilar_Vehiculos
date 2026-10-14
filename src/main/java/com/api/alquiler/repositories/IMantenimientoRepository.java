package com.api.alquiler.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.alquiler.models.MantenimientoModel;

@Repository
public interface IMantenimientoRepository extends JpaRepository<MantenimientoModel, Long> {

    boolean existsByVehiculo_Id(Long vehiculoId);

    List<MantenimientoModel> findByVehiculo_IdOrderByFechaMantenimientoDesc(Long vehiculoId);
}
