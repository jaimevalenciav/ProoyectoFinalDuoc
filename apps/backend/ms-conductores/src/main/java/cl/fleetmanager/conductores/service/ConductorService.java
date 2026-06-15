package cl.fleetmanager.conductores.service;

import cl.fleetmanager.conductores.dto.ConductorDto;
import cl.fleetmanager.conductores.entity.Conductor;
import cl.fleetmanager.conductores.repository.ConductorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConductorService {

    private final ConductorRepository repositorio;

    public Page<Conductor> obtenerTodos(
            String empresaId, String estado, String busqueda, int pagina, int tamano) {
        return repositorio.buscarPorFiltros(
            empresaId,
            (estado   != null && !estado.isBlank())   ? estado   : null,
            (busqueda != null && !busqueda.isBlank())  ? busqueda : null,
            PageRequest.of(pagina, tamano, Sort.by("nombre"))
        );
    }

    public Conductor obtenerPorId(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Conductor no encontrado: " + id));
    }

    public Conductor crear(String empresaId, ConductorDto datos) {
        return repositorio.save(Conductor.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .nombre(datos.getNombre())
            .rut(datos.getRut())
            .telefono(datos.getTelefono())
            .email(datos.getEmail())
            .categoriaLicencia(datos.getCategoriaLicencia())
            .vencimientoLicencia(datos.getVencimientoLicencia())
            .estado(datos.getEstado() != null ? datos.getEstado() : "ACTIVO")
            .fotoBase64(datos.getFotoBase64())
            .licenciaFrente(datos.getLicenciaFrente())
            .licenciaReverso(datos.getLicenciaReverso())
            .vencimientoCedula(datos.getVencimientoCedula())
            .build());
    }

    public Conductor actualizar(String id, ConductorDto datos) {
        Conductor c = obtenerPorId(id);
        if (datos.getNombre()             != null) c.setNombre(datos.getNombre());
        if (datos.getRut()                != null) c.setRut(datos.getRut());
        if (datos.getTelefono()           != null) c.setTelefono(datos.getTelefono());
        if (datos.getEmail()              != null) c.setEmail(datos.getEmail());
        if (datos.getCategoriaLicencia()  != null) c.setCategoriaLicencia(datos.getCategoriaLicencia());
        if (datos.getVencimientoLicencia()!= null) c.setVencimientoLicencia(datos.getVencimientoLicencia());
        if (datos.getEstado()             != null) c.setEstado(datos.getEstado());
        if (datos.getFotoBase64()         != null) c.setFotoBase64(datos.getFotoBase64());
        if (datos.getLicenciaFrente()     != null) c.setLicenciaFrente(datos.getLicenciaFrente());
        if (datos.getLicenciaReverso()    != null) c.setLicenciaReverso(datos.getLicenciaReverso());
        if (datos.getVencimientoCedula()  != null) c.setVencimientoCedula(datos.getVencimientoCedula());
        return repositorio.save(c);
    }

    public void eliminar(String id) {
        Conductor c = obtenerPorId(id);
        c.setEliminado(1);
        repositorio.save(c);
    }

    /** Score simplificado: basado en score actual + detalle para la vista */
    public Map<String, Object> obtenerScore(String id) {
        Conductor c = obtenerPorId(id);
        return Map.of(
            "score", c.getScoreConduccion() != null ? c.getScoreConduccion() : 100,
            "detalle", Map.of(
                "kmMes",          c.getKmMes()          != null ? c.getKmMes()          : 0,
                "horasMes",       c.getHorasMes()       != null ? c.getHorasMes()       : 0,
                "infraccionesMes",c.getInfraccionesMes()!= null ? c.getInfraccionesMes(): 0
            )
        );
    }
}
