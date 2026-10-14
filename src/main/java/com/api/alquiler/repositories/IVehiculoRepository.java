package com.api.alquiler.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.alquiler.models.VehiculoModel;

@Repository
public interface IVehiculoRepository extends JpaRepository<VehiculoModel, Long> {

    boolean existsByPlaca(String placa);

    boolean existsByOficina_Id(Long oficinaId);

    List<VehiculoModel> findByEstado(String estado);

    List<VehiculoModel> findByOficina_Id(Long oficinaId);

    List<VehiculoModel> findByEstadoAndOficina_Id(String estado, Long oficinaId);
}
