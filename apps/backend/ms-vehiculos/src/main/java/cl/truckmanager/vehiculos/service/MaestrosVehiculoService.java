package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.*;
import cl.truckmanager.vehiculos.entity.*;
import cl.truckmanager.vehiculos.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaestrosVehiculoService {

    private final SucursalRepository     sucursalRepo;
    private final MunicipalidadRepository municipalidadRepo;
    private final AseguradoraRepository  aseguradoraRepo;
    private final PlantaRevisionRepository plantaRepo;

    // ── Sucursales ───────────────────────────────────────────────

    public List<Sucursal> getSucursales(String empresaId) {
        return sucursalRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(empresaId, 0);
    }

    public Sucursal createSucursal(String empresaId, SucursalDto dto) {
        return sucursalRepo.save(Sucursal.builder()
            .empresaId(empresaId)
            .nombre(dto.getNombre())
            .direccion(dto.getDireccion())
            .ciudad(dto.getCiudad())
            .build());
    }

    public Sucursal updateSucursal(String id, SucursalDto dto) {
        Sucursal s = sucursalRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + id));
        if (dto.getNombre()    != null) s.setNombre(dto.getNombre());
        if (dto.getDireccion() != null) s.setDireccion(dto.getDireccion());
        if (dto.getCiudad()    != null) s.setCiudad(dto.getCiudad());
        return sucursalRepo.save(s);
    }

    public void deleteSucursal(String id) {
        Sucursal s = sucursalRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + id));
        s.setEliminado(1);
        sucursalRepo.save(s);
    }

    // ── Municipalidades ──────────────────────────────────────────

    public List<Municipalidad> getMunicipalidades(String empresaId) {
        return municipalidadRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(empresaId, 0);
    }

    public Municipalidad createMunicipalidad(String empresaId, MunicipalidadDto dto) {
        return municipalidadRepo.save(Municipalidad.builder()
            .empresaId(empresaId)
            .nombre(dto.getNombre())
            .region(dto.getRegion())
            .build());
    }

    public Municipalidad updateMunicipalidad(String id, MunicipalidadDto dto) {
        Municipalidad m = municipalidadRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Municipalidad no encontrada: " + id));
        if (dto.getNombre() != null) m.setNombre(dto.getNombre());
        if (dto.getRegion() != null) m.setRegion(dto.getRegion());
        return municipalidadRepo.save(m);
    }

    public void deleteMunicipalidad(String id) {
        Municipalidad m = municipalidadRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Municipalidad no encontrada: " + id));
        m.setEliminado(1);
        municipalidadRepo.save(m);
    }

    // ── Aseguradoras ─────────────────────────────────────────────

    public List<Aseguradora> getAseguradoras(String empresaId) {
        return aseguradoraRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(empresaId, 0);
    }

    public Aseguradora createAseguradora(String empresaId, AseguradoraDto dto) {
        return aseguradoraRepo.save(Aseguradora.builder()
            .empresaId(empresaId)
            .nombre(dto.getNombre())
            .rut(dto.getRut())
            .build());
    }

    public Aseguradora updateAseguradora(String id, AseguradoraDto dto) {
        Aseguradora a = aseguradoraRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Aseguradora no encontrada: " + id));
        if (dto.getNombre() != null) a.setNombre(dto.getNombre());
        if (dto.getRut()    != null) a.setRut(dto.getRut());
        return aseguradoraRepo.save(a);
    }

    public void deleteAseguradora(String id) {
        Aseguradora a = aseguradoraRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Aseguradora no encontrada: " + id));
        a.setEliminado(1);
        aseguradoraRepo.save(a);
    }

    // ── Plantas Revisión ─────────────────────────────────────────

    public List<PlantaRevision> getPlantasRevision(String empresaId) {
        return plantaRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(empresaId, 0);
    }

    public PlantaRevision createPlantaRevision(String empresaId, PlantaRevisionDto dto) {
        return plantaRepo.save(PlantaRevision.builder()
            .empresaId(empresaId)
            .nombre(dto.getNombre())
            .direccion(dto.getDireccion())
            .build());
    }

    public PlantaRevision updatePlantaRevision(String id, PlantaRevisionDto dto) {
        PlantaRevision p = plantaRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Planta no encontrada: " + id));
        if (dto.getNombre()    != null) p.setNombre(dto.getNombre());
        if (dto.getDireccion() != null) p.setDireccion(dto.getDireccion());
        return plantaRepo.save(p);
    }

    public void deletePlantaRevision(String id) {
        PlantaRevision p = plantaRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Planta no encontrada: " + id));
        p.setEliminado(1);
        plantaRepo.save(p);
    }
}
