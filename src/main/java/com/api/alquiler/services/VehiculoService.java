package com.api.alquiler.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.alquiler.models.OficinaModel;
import com.api.alquiler.models.VehiculoModel;
import com.api.alquiler.repositories.IAlquilerRepository;
import com.api.alquiler.repositories.IMantenimientoRepository;
import com.api.alquiler.repositories.IOficinaRepository;
import com.api.alquiler.repositories.IVehiculoRepository;

@Service
public class VehiculoService {

    private static final Set<String> ESTADOS = Set.of("DISPONIBLE", "ALQUILADO", "MANTENIMIENTO");

    private final IVehiculoRepository vehiculoRepository;
    private final IOficinaRepository oficinaRepository;
    private final IAlquilerRepository alquilerRepository;
    private final IMantenimientoRepository mantenimientoRepository;

    public VehiculoService(IVehiculoRepository vehiculoRepository,
                           IOficinaRepository oficinaRepository,
                           IAlquilerRepository alquilerRepository,
                           IMantenimientoRepository mantenimientoRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.oficinaRepository = oficinaRepository;
        this.alquilerRepository = alquilerRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    // Filtros opcionales: ?estado=DISPONIBLE&oficinaId=1
    public List<VehiculoModel> listar(String estado, Long oficinaId) {
        boolean hayEstado = estado != null && !estado.isBlank();
        String e = hayEstado ? estado.trim().toUpperCase() : null;

        if (hayEstado && oficinaId != null) {
            return vehiculoRepository.findByEstadoAndOficina_Id(e, oficinaId);
        }
        if (hayEstado) {
            return vehiculoRepository.findByEstado(e);
        }
        if (oficinaId != null) {
            return vehiculoRepository.findByOficina_Id(oficinaId);
        }
        return vehiculoRepository.findAll();
    }

    public VehiculoModel obtener(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un vehículo con id " + id));
    }

    public VehiculoModel crear(VehiculoModel vehiculo) {
        vehiculo.setId(null);
        validarDatos(vehiculo);

        String estado = normalizarEstado(vehiculo.getEstado(), "DISPONIBLE");
        if ("ALQUILADO".equals(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un vehículo nuevo no puede estar ALQUILADO; ese estado se asigna al activar un alquiler");
        }
        vehiculo.setEstado(estado);

        if (vehiculoRepository.existsByPlaca(vehiculo.getPlaca())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un vehículo con la placa " + vehiculo.getPlaca());
        }
        vehiculo.setOficina(buscarOficina(vehiculo));
        return vehiculoRepository.save(vehiculo);
    }

    public VehiculoModel actualizar(Long id, VehiculoModel datos) {
        VehiculoModel existente = obtener(id);
        validarDatos(datos);

        String nuevoEstado = normalizarEstado(datos.getEstado(), existente.getEstado());
        boolean cambia = !nuevoEstado.equals(existente.getEstado());
        if (cambia && ("ALQUILADO".equals(nuevoEstado) || "ALQUILADO".equals(existente.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado ALQUILADO lo controla el alquiler (activar / completar)");
        }

        if (!existente.getPlaca().equalsIgnoreCase(datos.getPlaca()) && vehiculoRepository.existsByPlaca(datos.getPlaca())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un vehículo con la placa " + datos.getPlaca());
        }

        existente.setMarca(datos.getMarca());
        existente.setModelo(datos.getModelo());
        existente.setAnio(datos.getAnio());
        existente.setPlaca(datos.getPlaca());
        existente.setTarifaDiaria(datos.getTarifaDiaria());
        existente.setEstado(nuevoEstado);
        existente.setOficina(buscarOficina(datos));
        return vehiculoRepository.save(existente);
    }

    // Regresa el vehículo a DISPONIBLE cuando termina su mantenimiento
    public VehiculoModel finalizarMantenimiento(Long id) {
        VehiculoModel vehiculo = obtener(id);
        if (!"MANTENIMIENTO".equals(vehiculo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo no está en mantenimiento");
        }
        vehiculo.setEstado("DISPONIBLE");
        return vehiculoRepository.save(vehiculo);
    }

    public void eliminar(Long id) {
        obtener(id);
        if (alquilerRepository.existsByVehiculo_Id(id) || mantenimientoRepository.existsByVehiculo_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el vehículo tiene alquileres o mantenimientos registrados");
        }
        vehiculoRepository.deleteById(id);
    }

    // ---------- utilidades ----------

    private void validarDatos(VehiculoModel v) {
        requerido(v.getMarca(), "marca");
        requerido(v.getModelo(), "modelo");
        requerido(v.getPlaca(), "placa");
        v.setPlaca(v.getPlaca().trim().toUpperCase());

        if (v.getAnio() == null || v.getAnio() < 1900) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'anio' es obligatorio y debe ser válido");
        }
        if (v.getTarifaDiaria() == null || v.getTarifaDiaria().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La 'tarifaDiaria' es obligatoria y debe ser mayor a 0");
        }
        if (v.getOficina() == null || v.getOficina().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere 'oficina.id'");
        }
    }

    private OficinaModel buscarOficina(VehiculoModel v) {
        Long id = v.getOficina().getId();
        return oficinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una oficina con id " + id));
    }

    private String normalizarEstado(String estado, String porDefecto) {
        if (estado == null || estado.isBlank()) {
            return porDefecto;
        }
        String e = estado.trim().toUpperCase();
        if (!ESTADOS.contains(e)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor inválido para 'estado'. Permitidos: " + ESTADOS);
        }
        return e;
    }

    private void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo '" + campo + "' es obligatorio");
        }
    }
}
