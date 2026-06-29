package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.CargaCombustibleDto;
import cl.truckmanager.operaciones.entity.AlertaCombustible;
import cl.truckmanager.operaciones.entity.CargaCombustible;
import cl.truckmanager.operaciones.repository.AlertaCombustibleRepository;
import cl.truckmanager.operaciones.repository.CargaCombustibleRepository;
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
    private final AlertaCombustibleRepository alertaRepo;

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

        // AdBlue y Carga Eléctrica no tienen consumo L/100km
        String tipo = dto.getTipoCombustible() != null ? dto.getTipoCombustible() : "Diesel";
        boolean calculaConsumo = !tipo.equalsIgnoreCase("AdBlue")
                              && !tipo.equalsIgnoreCase("Carga Eléctrica")
                              && !tipo.equalsIgnoreCase("Hidrógeno");
        BigDecimal consumo = calculaConsumo
            ? calcularConsumo(dto.getVehiculoId(), dto.getKmVehiculo(), litros)
            : null;

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
            .tipoCombustible(tipo)
            .build();

        CargaCombustible guardada = repositorio.save(carga);

        // ── auto-generar alerta si consumo es anómalo ──────────────
        if (consumo != null) {
            String mensaje = null;
            if (consumo.compareTo(new BigDecimal("35")) > 0) {
                mensaje = String.format(
                    "Consumo anómalo alto: %.1f L/100km para vehículo %s (carga del %s).",
                    consumo.doubleValue(), dto.getVehiculoId(), guardada.getFechaCarga());
            } else if (consumo.compareTo(new BigDecimal("5")) < 0) {
                mensaje = String.format(
                    "Consumo anómalo bajo: %.1f L/100km para vehículo %s (carga del %s).",
                    consumo.doubleValue(), dto.getVehiculoId(), guardada.getFechaCarga());
            }
            if (mensaje != null) {
                AlertaCombustible alerta = new AlertaCombustible();
                alerta.setId(UUID.randomUUID().toString());
                alerta.setEmpresaId(empresaId);
                alerta.setCargaId(guardada.getId());
                alerta.setVehiculoId(guardada.getVehiculoId());
                alerta.setTipo("warning");
                alerta.setIcono("local_gas_station");
                alerta.setMensaje(mensaje);
                alertaRepo.save(alerta);
            }
        }

        return guardada;
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
