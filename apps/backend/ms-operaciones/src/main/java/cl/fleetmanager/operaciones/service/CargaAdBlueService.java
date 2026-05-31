package cl.fleetmanager.operaciones.service;

import cl.fleetmanager.operaciones.dto.CargaAdBlueDto;
import cl.fleetmanager.operaciones.entity.CargaAdBlue;
import cl.fleetmanager.operaciones.repository.CargaAdBlueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CargaAdBlueService {

    private final CargaAdBlueRepository repositorio;

    public Page<CargaAdBlue> getAll(
        String empresaId, String vehiculoId,
        LocalDate desde, LocalDate hasta,
        int pagina, int tamano
    ) {
        return repositorio.buscarPorFiltros(
            empresaId,
            (vehiculoId != null && !vehiculoId.isBlank()) ? vehiculoId : null,
            desde, hasta,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaCarga"))
        );
    }

    public CargaAdBlue getById(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Carga AdBlue no encontrada: " + id));
    }

    public CargaAdBlue registrar(String empresaId, CargaAdBlueDto dto) {
        BigDecimal litros      = dto.getLitros()      != null ? dto.getLitros()      : BigDecimal.ZERO;
        BigDecimal precioLitro = dto.getPrecioLitro() != null ? dto.getPrecioLitro() : BigDecimal.ZERO;
        BigDecimal costoTotal  = litros.multiply(precioLitro).setScale(2, RoundingMode.HALF_UP);

        CargaAdBlue carga = CargaAdBlue.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .vehiculoId(dto.getVehiculoId())
            .conductorId(dto.getConductorId())
            .numDocumento(dto.getNumDocumento())
            .proveedor(dto.getProveedor() != null ? dto.getProveedor() : "Sin especificar")
            .estacion(dto.getEstacion())
            .fechaCarga(dto.getFechaCarga() != null ? dto.getFechaCarga() : LocalDate.now())
            .litros(litros)
            .precioLitro(precioLitro)
            .costoTotal(costoTotal)
            .kmVehiculo(dto.getKmVehiculo() != null ? dto.getKmVehiculo() : 0L)
            .pctDiesel(null)   // se podría calcular en el futuro correlacionando con diésel
            .build();

        return repositorio.save(carga);
    }

    public void eliminar(String id) {
        repositorio.deleteById(id);
    }

    public Optional<CargaAdBlue> getUltimaCarga(String vehiculoId) {
        return repositorio.findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(vehiculoId);
    }

    public List<CargaAdBlue> getAnomalias(String empresaId) {
        return repositorio.findAnomalias(empresaId);
    }
}
