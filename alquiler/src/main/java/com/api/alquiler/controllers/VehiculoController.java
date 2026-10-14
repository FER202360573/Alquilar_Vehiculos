package com.api.alquiler.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.alquiler.models.VehiculoModel;
import com.api.alquiler.services.VehiculoService;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    // Ejemplos: /api/vehiculos  |  /api/vehiculos?estado=DISPONIBLE  |  /api/vehiculos?oficinaId=1
    @GetMapping
    public List<VehiculoModel> listar(@RequestParam(required = false) String estado,
                                      @RequestParam(required = false) Long oficinaId) {
        return vehiculoService.listar(estado, oficinaId);
    }

    @GetMapping("/{id}")
    public VehiculoModel obtener(@PathVariable Long id) {
        return vehiculoService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<VehiculoModel> crear(@RequestBody VehiculoModel vehiculo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.crear(vehiculo));
    }

    @PutMapping("/{id}")
    public VehiculoModel actualizar(@PathVariable Long id, @RequestBody VehiculoModel vehiculo) {
        return vehiculoService.actualizar(id, vehiculo);
    }

    @PutMapping("/{id}/finalizar-mantenimiento")
    public VehiculoModel finalizarMantenimiento(@PathVariable Long id) {
        return vehiculoService.finalizarMantenimiento(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
