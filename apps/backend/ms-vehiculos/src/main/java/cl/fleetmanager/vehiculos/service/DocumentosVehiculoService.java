package cl.fleetmanager.vehiculos.service;

import cl.fleetmanager.vehiculos.dto.*;
import cl.fleetmanager.vehiculos.entity.*;
import cl.fleetmanager.vehiculos.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentosVehiculoService {

    private final PermisoCirculacionRepository permisoRepo;
    private final SeguroSoapRepository         seguroRepo;
    private final RevisionTecnicaRepository    revisionRepo;

    // ── Permiso Circulación ──────────────────────────────────────

    public List<PermisoCirculacion> getPermisos(String vehiculoId) {
        return permisoRepo.findByVehiculoIdOrderByFechaPagoDescCreatedAtDesc(vehiculoId);
    }

    public PermisoCirculacion createPermiso(String empresaId, PermisoCirculacionDto dto) {
        PermisoCirculacion p = new PermisoCirculacion();
        p.setVehiculoId(dto.getVehiculoId());
        p.setEmpresaId(empresaId);
        p.setMunicipalidadId(dto.getMunicipalidadId());
        p.setFechaPago(dto.getFechaPago());
        p.setValor(dto.getValor());
        p.setFechaVencimiento(dto.getFechaVencimiento());
        p.setDocumento(dto.getDocumento());
        return permisoRepo.save(p);
    }

    public void deletePermiso(String id) {
        if (!permisoRepo.existsById(id))
            throw new EntityNotFoundException("Permiso no encontrado: " + id);
        permisoRepo.deleteById(id);
    }

    // ── Seguro SOAP ──────────────────────────────────────────────

    public List<SeguroSoap> getSeguros(String vehiculoId) {
        return seguroRepo.findByVehiculoIdOrderByFechaEmisionDescCreatedAtDesc(vehiculoId);
    }

    public SeguroSoap createSeguro(String empresaId, SeguroSoapDto dto) {
        SeguroSoap s = new SeguroSoap();
        s.setVehiculoId(dto.getVehiculoId());
        s.setEmpresaId(empresaId);
        s.setAseguradoraId(dto.getAseguradoraId());
        s.setFechaEmision(dto.getFechaEmision());
        s.setValor(dto.getValor());
        s.setFechaVencimiento(dto.getFechaVencimiento());
        s.setPoliza(dto.getPoliza());
        return seguroRepo.save(s);
    }

    public void deleteSeguro(String id) {
        if (!seguroRepo.existsById(id))
            throw new EntityNotFoundException("Seguro SOAP no encontrado: " + id);
        seguroRepo.deleteById(id);
    }

    // ── Revisión Técnica ─────────────────────────────────────────

    public List<RevisionTecnica> getRevisiones(String vehiculoId) {
        return revisionRepo.findByVehiculoIdOrderByFechaRevisionDescCreatedAtDesc(vehiculoId);
    }

    public RevisionTecnica createRevision(String empresaId, RevisionTecnicaDto dto) {
        RevisionTecnica r = new RevisionTecnica();
        r.setVehiculoId(dto.getVehiculoId());
        r.setEmpresaId(empresaId);
        r.setPlantaId(dto.getPlantaId());
        r.setFechaRevision(dto.getFechaRevision());
        r.setValor(dto.getValor());
        r.setFechaVencimiento(dto.getFechaVencimiento());
        r.setResultado(dto.getResultado());
        return revisionRepo.save(r);
    }

    public void deleteRevision(String id) {
        if (!revisionRepo.existsById(id))
            throw new EntityNotFoundException("Revisión técnica no encontrada: " + id);
        revisionRepo.deleteById(id);
    }
}
