package com.api.alquiler.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.alquiler.models.OficinaModel;
import com.api.alquiler.repositories.IOficinaRepository;
import com.api.alquiler.repositories.IVehiculoRepository;

@Service
public class OficinaService {

    private final IOficinaRepository oficinaRepository;
    private final IVehiculoRepository vehiculoRepository;

    public OficinaService(IOficinaRepository oficinaRepository, IVehiculoRepository vehiculoRepository) {
        this.oficinaRepository = oficinaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<OficinaModel> listar() {
        return oficinaRepository.findAll();
    }

    public OficinaModel obtener(Long id) {
        return oficinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una oficina con id " + id));
    }

    public OficinaModel crear(OficinaModel oficina) {
        oficina.setId(null);
        validar(oficina);
        return oficinaRepository.save(oficina);
    }

    public OficinaModel actualizar(Long id, OficinaModel datos) {
        OficinaModel existente = obtener(id);
        validar(datos);
        existente.setNombreOficina(datos.getNombreOficina());
        existente.setDireccion(datos.getDireccion());
        existente.setCiudad(datos.getCiudad());
        existente.setRegion(datos.getRegion());
        return oficinaRepository.save(existente);
    }

    public void eliminar(Long id) {
        obtener(id);
        if (vehiculoRepository.existsByOficina_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar: la oficina tiene vehículos asignados");
        }
        oficinaRepository.deleteById(id);
    }

    private void validar(OficinaModel o) {
        requerido(o.getNombreOficina(), "nombreOficina");
        requerido(o.getDireccion(), "direccion");
        requerido(o.getCiudad(), "ciudad");
        requerido(o.getRegion(), "region");
    }

    private void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo '" + campo + "' es obligatorio");
        }
    }
}
