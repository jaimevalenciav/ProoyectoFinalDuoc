package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.MecanicoDto;
import cl.truckmanager.conductores.entity.Mecanico;
import cl.truckmanager.conductores.repository.MecanicoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MecanicoService {

    private final MecanicoRepository repositorio;

    public Page<Mecanico> obtenerTodos(String empresaId, String busqueda, Integer activo,
                                       int pagina, int tamano) {
        return repositorio.buscarPorFiltros(
            empresaId,
            (busqueda != null && !busqueda.isBlank()) ? busqueda : null,
            activo,
            PageRequest.of(pagina, tamano)
        );
    }

    public List<Mecanico> obtenerActivos(String empresaId) {
        return repositorio.findByEmpresaIdAndActivoOrderByNombre(empresaId, 1);
    }

    public Mecanico obtenerPorId(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Mecánico no encontrado: " + id));
    }

    public Mecanico crear(String empresaId, MecanicoDto dto) {
        return repositorio.save(Mecanico.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .nombre(dto.getNombre())
            .rut(dto.getRut())
            .especialidad(dto.getEspecialidad())
            .telefono(dto.getTelefono())
            .email(dto.getEmail())
            .activo(dto.getActivo() != null ? dto.getActivo() : 1)
            .observacion(dto.getObservacion())
            .build());
    }

    public Mecanico actualizar(String id, MecanicoDto dto) {
        Mecanico m = obtenerPorId(id);
        m.setNombre(dto.getNombre());
        if (dto.getRut()          != null) m.setRut(dto.getRut());
        if (dto.getEspecialidad() != null) m.setEspecialidad(dto.getEspecialidad());
        if (dto.getTelefono()     != null) m.setTelefono(dto.getTelefono());
        if (dto.getEmail()        != null) m.setEmail(dto.getEmail());
        if (dto.getActivo()       != null) m.setActivo(dto.getActivo());
        if (dto.getObservacion()  != null) m.setObservacion(dto.getObservacion());
        return repositorio.save(m);
    }

    public void eliminar(String id) {
        Mecanico m = obtenerPorId(id);
        m.setActivo(0);
        repositorio.save(m);
    }
}
