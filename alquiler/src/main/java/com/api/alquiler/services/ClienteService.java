package com.api.alquiler.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.alquiler.models.ClienteModel;
import com.api.alquiler.repositories.IAlquilerRepository;
import com.api.alquiler.repositories.IClienteRepository;

@Service
public class ClienteService {

    private final IClienteRepository clienteRepository;
    private final IAlquilerRepository alquilerRepository;

    public ClienteService(IClienteRepository clienteRepository, IAlquilerRepository alquilerRepository) {
        this.clienteRepository = clienteRepository;
        this.alquilerRepository = alquilerRepository;
    }

    public List<ClienteModel> listar() {
        return clienteRepository.findAll();
    }

    public ClienteModel obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un cliente con id " + id));
    }

    public ClienteModel crear(ClienteModel cliente) {
        cliente.setId(null);
        validar(cliente);
        if (clienteRepository.existsByEmail(cliente.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con el email " + cliente.getEmail());
        }
        if (clienteRepository.existsByLicencia(cliente.getLicencia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con la licencia " + cliente.getLicencia());
        }
        return clienteRepository.save(cliente);
    }

    public ClienteModel actualizar(Long id, ClienteModel datos) {
        ClienteModel existente = obtener(id);
        validar(datos);
        if (!existente.getEmail().equalsIgnoreCase(datos.getEmail()) && clienteRepository.existsByEmail(datos.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con el email " + datos.getEmail());
        }
        if (!existente.getLicencia().equalsIgnoreCase(datos.getLicencia())
                && clienteRepository.existsByLicencia(datos.getLicencia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con la licencia " + datos.getLicencia());
        }
        existente.setNombre(datos.getNombre());
        existente.setApellido(datos.getApellido());
        existente.setEmail(datos.getEmail());
        existente.setLicencia(datos.getLicencia());
        existente.setTelefono(datos.getTelefono());
        return clienteRepository.save(existente);
    }

    public void eliminar(Long id) {
        obtener(id);
        if (alquilerRepository.existsByCliente_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar: el cliente tiene alquileres registrados");
        }
        clienteRepository.deleteById(id);
    }

    private void validar(ClienteModel c) {
        requerido(c.getNombre(), "nombre");
        requerido(c.getApellido(), "apellido");
        requerido(c.getEmail(), "email");
        requerido(c.getLicencia(), "licencia");
        c.setEmail(c.getEmail().trim());
        c.setLicencia(c.getLicencia().trim().toUpperCase());
    }

    private void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo '" + campo + "' es obligatorio");
        }
    }
}
