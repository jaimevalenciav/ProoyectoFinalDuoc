package cl.fleetmanager.operaciones.service;

import cl.fleetmanager.operaciones.dto.FacturarDto;
import cl.fleetmanager.operaciones.entity.Cliente;
import cl.fleetmanager.operaciones.entity.Factura;
import cl.fleetmanager.operaciones.entity.Servicio;
import cl.fleetmanager.operaciones.repository.ClienteRepository;
import cl.fleetmanager.operaciones.repository.FacturaRepository;
import cl.fleetmanager.operaciones.repository.ServicioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {

    private final FacturaRepository    facturaRepo;
    private final ServicioRepository   servicioRepo;
    private final ClienteRepository    clienteRepo;

    public Page<Factura> getAll(String empresaId, String clienteId, String estado, int pagina, int tamano) {
        Page<Factura> page = facturaRepo.buscarPorFiltros(
            empresaId,
            (clienteId != null && !clienteId.isBlank()) ? clienteId : null,
            (estado    != null && !estado.isBlank())    ? estado    : null,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaEmision"))
        );

        // Enriquecer con razón social
        page.getContent().forEach(f -> {
            clienteRepo.findById(f.getClienteId()).ifPresent(c -> f.setClienteRazonSocial(c.getRazonSocial()));
        });

        return page;
    }

    public Factura getById(String id) {
        Factura f = facturaRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + id));
        clienteRepo.findById(f.getClienteId()).ifPresent(c -> f.setClienteRazonSocial(c.getRazonSocial()));
        return f;
    }

    public Factura facturar(String empresaId, FacturarDto datos) {
        List<Servicio> servicios = servicioRepo.findByIdInAndEmpresaId(datos.getServicioIds(), empresaId);

        if (servicios.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron servicios válidos para facturar.");
        }

        // Validar que todos sean del mismo cliente
        long clientesDistintos = servicios.stream().map(Servicio::getClienteId).distinct().count();
        if (clientesDistintos > 1) {
            throw new IllegalArgumentException("Todos los servicios deben pertenecer al mismo cliente.");
        }

        // Validar que ninguno esté ya facturado
        boolean hayFacturados = servicios.stream().anyMatch(s -> s.getFacturado() != null && s.getFacturado() == 1);
        if (hayFacturados) {
            throw new IllegalArgumentException("Uno o más servicios ya están facturados.");
        }

        // Validar que el clienteId coincida
        String clienteIdReal = servicios.get(0).getClienteId();
        if (!clienteIdReal.equals(datos.getClienteId())) {
            throw new IllegalArgumentException("El clienteId no coincide con los servicios seleccionados.");
        }

        // Calcular totales
        BigDecimal subtotal = servicios.stream()
            .map(s -> s.getValorNeto() != null ? s.getValorNeto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal iva = servicios.stream()
            .map(s -> s.getIva() != null ? s.getIva() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = servicios.stream()
            .map(s -> s.getValorTotal() != null ? s.getValorTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generar número de factura
        String numFactura = generarNumero(empresaId);

        // Obtener razón social del cliente
        Cliente cliente = clienteRepo.findById(clienteIdReal).orElse(null);

        // Crear factura
        Factura factura = Factura.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .clienteId(clienteIdReal)
            .numFactura(numFactura)
            .subtotal(subtotal)
            .iva(iva)
            .total(total)
            .estado("EMITIDA")
            .notas(datos.getNotas())
            .build();

        Factura guardada = facturaRepo.save(factura);

        // Actualizar servicios
        servicios.forEach(s -> {
            s.setFacturado(1);
            s.setFacturaId(guardada.getId());
            servicioRepo.save(s);
        });

        if (cliente != null) guardada.setClienteRazonSocial(cliente.getRazonSocial());
        return guardada;
    }

    public Factura anular(String id) {
        Factura factura = facturaRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + id));

        if ("ANULADA".equals(factura.getEstado())) {
            throw new IllegalArgumentException("La factura ya está anulada.");
        }

        factura.setEstado("ANULADA");
        Factura guardada = facturaRepo.save(factura);

        // Desvincular servicios asociados
        var servicios = servicioRepo.findByFacturaId(id);

        servicios.forEach(s -> {
            s.setFacturado(0);
            s.setFacturaId(null);
            servicioRepo.save(s);
        });

        clienteRepo.findById(guardada.getClienteId())
            .ifPresent(c -> guardada.setClienteRazonSocial(c.getRazonSocial()));

        return guardada;
    }

    private String generarNumero(String empresaId) {
        String ultimo = facturaRepo.ultimoNumero(empresaId);
        int siguiente = 1;
        if (ultimo != null && ultimo.contains("-")) {
            try { siguiente = Integer.parseInt(ultimo.split("-")[1]) + 1; } catch (Exception ignored) {}
        }
        return String.format("FAC-%04d", siguiente);
    }
}
