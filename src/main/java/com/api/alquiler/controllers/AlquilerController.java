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

import com.api.alquiler.models.AlquilerModel;
import com.api.alquiler.services.AlquilerService;

@RestController
@RequestMapping("/api/alquileres")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    // Ejemplo: /api/alquileres?estado=ACTIVO
    @GetMapping
    public List<AlquilerModel> listar(@RequestParam(required = false) String estado) {
        return alquilerService.listar(estado);
    }

    @GetMapping("/{id}")
    public AlquilerModel obtener(@PathVariable Long id) {
        return alquilerService.obtener(id);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<AlquilerModel> listarPorCliente(@PathVariable Long clienteId) {
        return alquilerService.listarPorCliente(clienteId);
    }

    @PostMapping
    public ResponseEntity<AlquilerModel> crear(@RequestBody AlquilerModel alquiler) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alquilerService.crear(alquiler));
    }

    @PutMapping("/{id}/activar")
    public AlquilerModel activar(@PathVariable Long id) {
        return alquilerService.activar(id);
    }

    @PutMapping("/{id}/completar")
    public AlquilerModel completar(@PathVariable Long id) {
        return alquilerService.completar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        alquilerService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
