package cl.fleetmanager.almacen.service;

import cl.fleetmanager.almacen.dto.AjusteStockDto;
import cl.fleetmanager.almacen.dto.RepuestoDto;
import cl.fleetmanager.almacen.entity.MovimientoStock;
import cl.fleetmanager.almacen.entity.Repuesto;
import cl.fleetmanager.almacen.repository.MovimientoStockRepository;
import cl.fleetmanager.almacen.repository.RepuestoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RepuestoService {

    private final RepuestoRepository repuestoRepo;
    private final MovimientoStockRepository movimientoRepo;

    public Page<Repuesto> getAll(String emp, String cat, String q, int page, int size) {
        return repuestoRepo.buscar(emp, cat, q, PageRequest.of(page, size));
    }

    public List<Repuesto> getAllActivos(String emp) {
        return repuestoRepo.findAllActivosByEmpresa(emp);
    }

    public List<Repuesto> getBajoStock(String emp) {
        return repuestoRepo.findBajoStock(emp);
    }

    public Repuesto getById(String id) {
        return repuestoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado: " + id));
    }

    public Repuesto crear(String emp, RepuestoDto dto) {
        Repuesto r = Repuesto.builder()
                .empresaId(emp)
                .codigo(dto.getCodigo())
                .descripcion(dto.getDescripcion())
                .categoria(dto.getCategoria())
                .unidad(dto.getUnidad())
                .stockActual(dto.getStockActual())
                .stockMinimo(dto.getStockMinimo())
                .precioUnitario(dto.getPrecioUnitario())
                .proveedor(dto.getProveedor())
                .build();
        return repuestoRepo.save(r);
    }

    public Repuesto actualizar(String id, RepuestoDto dto) {
        Repuesto r = getById(id);
        r.setCodigo(dto.getCodigo());
        r.setDescripcion(dto.getDescripcion());
        r.setCategoria(dto.getCategoria());
        if (dto.getUnidad() != null)        r.setUnidad(dto.getUnidad());
        if (dto.getStockMinimo() != null)   r.setStockMinimo(dto.getStockMinimo());
        if (dto.getPrecioUnitario() != null) r.setPrecioUnitario(dto.getPrecioUnitario());
        r.setProveedor(dto.getProveedor());
        return repuestoRepo.save(r);
    }

    public Repuesto ajustarStock(String id, AjusteStockDto dto) {
        Repuesto r = getById(id);
        BigDecimal cantidad = dto.getCantidad();
        BigDecimal stockAnterior = r.getStockActual() != null ? r.getStockActual() : BigDecimal.ZERO;

        String tipo = dto.getTipo();

        if ("SALIDA".equalsIgnoreCase(tipo)) {
            if (stockAnterior.compareTo(cantidad) < 0) {
                throw new IllegalStateException("Stock insuficiente");
            }
            r.setStockActual(stockAnterior.subtract(cantidad));
        } else if ("ENTRADA".equalsIgnoreCase(tipo)) {
            r.setStockActual(stockAnterior.add(cantidad));
        } else {
            // AJUSTE: cantidad es el nuevo valor absoluto
            r.setStockActual(cantidad);
        }

        BigDecimal stockNuevo = r.getStockActual();
        BigDecimal costoTotal = r.getPrecioUnitario() != null
                ? r.getPrecioUnitario().multiply(cantidad)
                : BigDecimal.ZERO;

        MovimientoStock mov = MovimientoStock.builder()
                .empresaId(r.getEmpresaId())
                .repuesto(r)
                .tipo(tipo.toUpperCase())
                .cantidad(cantidad)
                .precioUnit(r.getPrecioUnitario())
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .costoTotal(costoTotal)
                .referencia(dto.getReferencia())
                .documento(dto.getDocumento())
                .otId(dto.getOtId())
                .build();

        movimientoRepo.save(mov);
        return repuestoRepo.save(r);
    }

    public void eliminar(String id) {
        Repuesto r = getById(id);
        r.setEliminado(1);
        repuestoRepo.save(r);
    }
}
