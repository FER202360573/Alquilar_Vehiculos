package com.api.alquiler.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.alquiler.models.AlquilerModel;

@Repository
public interface IAlquilerRepository extends JpaRepository<AlquilerModel, Long> {

    boolean existsByCliente_Id(Long clienteId);

    boolean existsByVehiculo_Id(Long vehiculoId);

    List<AlquilerModel> findByEstado(String estado);

    List<AlquilerModel> findByCliente_Id(Long clienteId);

    // true si el vehículo ya tiene un alquiler pendiente o activo que se traslapa con las fechas
    @Query("SELECT COUNT(a) > 0 FROM AlquilerModel a WHERE a.vehiculo.id = :vehiculoId "
            + "AND a.estado IN ('PENDIENTE', 'ACTIVO') AND a.fechaInicio < :fin AND a.fechaFin > :inicio")
    boolean hayTraslape(@Param("vehiculoId") Long vehiculoId,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin);
}
