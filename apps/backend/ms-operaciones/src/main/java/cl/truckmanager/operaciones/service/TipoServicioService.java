package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.TipoServicioDto;
import cl.truckmanager.operaciones.entity.TipoServicio;
import cl.truckmanager.operaciones.repository.TipoServicioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TipoServicioService {

    private final TipoServicioRepository repositorio;

    public List<TipoServicio> getAll(String empresaId) {
        return repositorio.findByEmpresaIdAndActivo(empresaId, 1);
    }

    public List<TipoServicio> getAllIncluidos(String empresaId) {
        return repositorio.findByEmpresaId(empresaId);
    }

    public TipoServicio crear(String empresaId, TipoServicioDto datos) {
        TipoServicio tipo = TipoServicio.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .codigo(datos.getCodigo())
            .nombre(datos.getNombre())
            .activo(datos.getActivo() != null ? datos.getActivo() : 1)
            .build();
        return repositorio.save(tipo);
    }

    public TipoServicio actualizar(String id, TipoServicioDto datos) {
        TipoServicio tipo = repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Tipo de servicio no encontrado: " + id));
        if (datos.getCodigo() != null) tipo.setCodigo(datos.getCodigo());
        if (datos.getNombre() != null) tipo.setNombre(datos.getNombre());
        if (datos.getActivo() != null) tipo.setActivo(datos.getActivo());
        return repositorio.save(tipo);
    }
}
