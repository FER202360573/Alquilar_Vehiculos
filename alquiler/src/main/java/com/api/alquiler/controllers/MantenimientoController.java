package com.api.alquiler.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.alquiler.models.MantenimientoModel;
import com.api.alquiler.services.MantenimientoService;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    public MantenimientoController(MantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @GetMapping
    public List<MantenimientoModel> listar() {
        return mantenimientoService.listar();
    }

    @GetMapping("/{id}")
    public MantenimientoModel obtener(@PathVariable Long id) {
        return mantenimientoService.obtener(id);
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public List<MantenimientoModel> historial(@PathVariable Long vehiculoId) {
        return mantenimientoService.historial(vehiculoId);
    }

    @PostMapping
    public ResponseEntity<MantenimientoModel> crear(@RequestBody MantenimientoModel mantenimiento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mantenimientoService.crear(mantenimiento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mantenimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
