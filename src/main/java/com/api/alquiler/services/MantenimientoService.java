package com.api.alquiler.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.alquiler.models.MantenimientoModel;
import com.api.alquiler.models.VehiculoModel;
import com.api.alquiler.repositories.IMantenimientoRepository;
import com.api.alquiler.repositories.IVehiculoRepository;

@Service
public class MantenimientoService {

    private final IMantenimientoRepository mantenimientoRepository;
    private final IVehiculoRepository vehiculoRepository;

    public MantenimientoService(IMantenimientoRepository mantenimientoRepository,
                                IVehiculoRepository vehiculoRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<MantenimientoModel> listar() {
        return mantenimientoRepository.findAll();
    }

    public MantenimientoModel obtener(Long id) {
        return mantenimientoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un mantenimiento con id " + id));
    }

    // Historial de mantenimientos de un vehículo (más recientes primero)
    public List<MantenimientoModel> historial(Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un vehículo con id " + vehiculoId);
        }
        return mantenimientoRepository.findByVehiculo_IdOrderByFechaMantenimientoDesc(vehiculoId);
    }

    // Registra el mantenimiento y deja el vehículo en estado MANTENIMIENTO
    @Transactional
    public MantenimientoModel crear(MantenimientoModel request) {
        if (request == null || request.getVehiculo() == null || request.getVehiculo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere 'vehiculo.id'");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'descripcion' es obligatorio");
        }
        if (request.getCosto() == null || request.getCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 'costo' es obligatorio y no puede ser negativo");
        }
        if (request.getKilometraje() != null && request.getKilometraje() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 'kilometraje' no puede ser negativo");
        }

        VehiculoModel vehiculo = vehiculoRepository.findById(request.getVehiculo().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un vehículo con id " + request.getVehiculo().getId()));
        if ("ALQUILADO".equals(vehiculo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo está ALQUILADO; completa el alquiler primero");
        }

        vehiculo.setEstado("MANTENIMIENTO");
        vehiculoRepository.save(vehiculo);

        MantenimientoModel mantenimiento = new MantenimientoModel();
        mantenimiento.setVehiculo(vehiculo);
        mantenimiento.setDescripcion(request.getDescripcion());
        mantenimiento.setCosto(request.getCosto());
        mantenimiento.setKilometraje(request.getKilometraje());
        mantenimiento.setFechaMantenimiento(
                request.getFechaMantenimiento() != null ? request.getFechaMantenimiento() : LocalDate.now());
        return mantenimientoRepository.save(mantenimiento);
    }

    public void eliminar(Long id) {
        obtener(id);
        mantenimientoRepository.deleteById(id);
    }
}
