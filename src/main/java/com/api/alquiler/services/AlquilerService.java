package com.api.alquiler.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.alquiler.models.AlquilerModel;
import com.api.alquiler.models.ClienteModel;
import com.api.alquiler.models.VehiculoModel;
import com.api.alquiler.repositories.IAlquilerRepository;
import com.api.alquiler.repositories.IClienteRepository;
import com.api.alquiler.repositories.IVehiculoRepository;

@Service
public class AlquilerService {

    private final IAlquilerRepository alquilerRepository;
    private final IVehiculoRepository vehiculoRepository;
    private final IClienteRepository clienteRepository;

    public AlquilerService(IAlquilerRepository alquilerRepository,
                           IVehiculoRepository vehiculoRepository,
                           IClienteRepository clienteRepository) {
        this.alquilerRepository = alquilerRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<AlquilerModel> listar(String estado) {
        if (estado == null || estado.isBlank()) {
            return alquilerRepository.findAll();
        }
        return alquilerRepository.findByEstado(estado.trim().toUpperCase());
    }

    public AlquilerModel obtener(Long id) {
        return alquilerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un alquiler con id " + id));
    }

    public List<AlquilerModel> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un cliente con id " + clienteId);
        }
        return alquilerRepository.findByCliente_Id(clienteId);
    }

    // Crea el alquiler en estado PENDIENTE y calcula el precio total
    @Transactional
    public AlquilerModel crear(AlquilerModel request) {
        if (request == null || request.getVehiculo() == null || request.getVehiculo().getId() == null
                || request.getCliente() == null || request.getCliente().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren 'vehiculo.id' y 'cliente.id'");
        }
        LocalDate inicio = request.getFechaInicio();
        LocalDate fin = request.getFechaFin();
        if (inicio == null || fin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren 'fechaInicio' y 'fechaFin' (AAAA-MM-DD)");
        }
        if (!fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La 'fechaFin' debe ser posterior a la 'fechaInicio'");
        }

        VehiculoModel vehiculo = vehiculoRepository.findById(request.getVehiculo().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un vehículo con id " + request.getVehiculo().getId()));
        ClienteModel cliente = clienteRepository.findById(request.getCliente().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un cliente con id " + request.getCliente().getId()));

        if (!"DISPONIBLE".equals(vehiculo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El vehículo no está disponible (estado actual: " + vehiculo.getEstado() + ")");
        }
        if (alquilerRepository.hayTraslape(vehiculo.getId(), inicio, fin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo ya tiene un alquiler en esas fechas");
        }

        long dias = ChronoUnit.DAYS.between(inicio, fin);

        AlquilerModel alquiler = new AlquilerModel();
        alquiler.setVehiculo(vehiculo);
        alquiler.setCliente(cliente);
        alquiler.setFechaInicio(inicio);
        alquiler.setFechaFin(fin);
        alquiler.setPrecioTotal(vehiculo.getTarifaDiaria().multiply(BigDecimal.valueOf(dias)));
        alquiler.setEstado("PENDIENTE");
        return alquilerRepository.save(alquiler);
    }

    // PENDIENTE -> ACTIVO (el cliente recoge el vehículo)
    @Transactional
    public AlquilerModel activar(Long id) {
        AlquilerModel alquiler = obtener(id);
        if (!"PENDIENTE".equals(alquiler.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede activar un alquiler PENDIENTE");
        }
        VehiculoModel vehiculo = alquiler.getVehiculo();
        if (!"DISPONIBLE".equals(vehiculo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El vehículo no está disponible (estado actual: " + vehiculo.getEstado() + ")");
        }
        vehiculo.setEstado("ALQUILADO");
        vehiculoRepository.save(vehiculo);

        alquiler.setEstado("ACTIVO");
        return alquilerRepository.save(alquiler);
    }

    // ACTIVO -> COMPLETADO (el cliente devuelve el vehículo)
    @Transactional
    public AlquilerModel completar(Long id) {
        AlquilerModel alquiler = obtener(id);
        if (!"ACTIVO".equals(alquiler.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede completar un alquiler ACTIVO");
        }
        VehiculoModel vehiculo = alquiler.getVehiculo();
        vehiculo.setEstado("DISPONIBLE");
        vehiculoRepository.save(vehiculo);

        alquiler.setEstado("COMPLETADO");
        return alquilerRepository.save(alquiler);
    }

    // Solo se pueden eliminar (cancelar) alquileres que aún no empiezan
    public void eliminar(Long id) {
        AlquilerModel alquiler = obtener(id);
        if (!"PENDIENTE".equals(alquiler.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede eliminar un alquiler PENDIENTE");
        }
        alquilerRepository.delete(alquiler);
    }
}
