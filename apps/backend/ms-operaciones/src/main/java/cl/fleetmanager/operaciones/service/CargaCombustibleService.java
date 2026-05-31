package cl.fleetmanager.operaciones.service;

import cl.fleetmanager.operaciones.dto.CargaCombustibleDto;
import cl.fleetmanager.operaciones.entity.CargaCombustible;
import cl.fleetmanager.operaciones.repository.CargaCombustibleRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CargaCombustibleService {

    private final CargaCombustibleRepository repositorio;

    public Page<CargaCombustible> getAll(
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

    public CargaCombustible getById(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Carga no encontrada: " + id));
    }

    public CargaCombustible registrar(String empresaId, CargaCombustibleDto dto) {
        BigDecimal litros      = dto.getLitros()      != null ? dto.getLitros()      : BigDecimal.ZERO;
        BigDecimal precioLitro = dto.getPrecioLitro() != null ? dto.getPrecioLitro() : BigDecimal.ZERO;
        BigDecimal costoTotal  = litros.multiply(precioLitro).setScale(2, RoundingMode.HALF_UP);
        BigDecimal consumo     = calcularConsumo(dto.getVehiculoId(), dto.getKmVehiculo(), litros);

        CargaCombustible carga = CargaCombustible.builder()
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
            .consumo100km(consumo)
            .build();

        return repositorio.save(carga);
    }

    public void eliminar(String id) {
        repositorio.deleteById(id);
    }

    public java.util.Optional<CargaCombustible> getUltimaCarga(String vehiculoId) {
        return repositorio.findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(vehiculoId);
    }

    public List<CargaCombustible> getAnomalias(String empresaId) {
        return repositorio.findAnomalias(empresaId);
    }

    // ── privado ─────────────────────────────────────────────

    private BigDecimal calcularConsumo(String vehiculoId, Long kmActual, BigDecimal litros) {
        if (vehiculoId == null || kmActual == null || litros == null
                || litros.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        List<CargaCombustible> previas = repositorio
            .findTop2ByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(vehiculoId);

        if (previas.isEmpty()) return null;

        CargaCombustible anterior = previas.get(0);
        if (anterior.getKmVehiculo() == null || anterior.getKmVehiculo() >= kmActual) return null;

        long kmRecorridos = kmActual - anterior.getKmVehiculo();
        if (kmRecorridos <= 0) return null;

        return litros
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(kmRecorridos), 2, RoundingMode.HALF_UP);
    }
}
