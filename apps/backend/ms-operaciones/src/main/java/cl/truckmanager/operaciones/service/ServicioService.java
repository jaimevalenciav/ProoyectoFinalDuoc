package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.AsignarServicioDto;
import cl.truckmanager.operaciones.dto.ServicioDto;
import cl.truckmanager.operaciones.entity.Servicio;
import cl.truckmanager.operaciones.repository.ServicioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServicioService {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.19");

    private final ServicioRepository repositorio;

    public Page<Servicio> getAll(
        String    empresaId,
        String    estado,
        String    clienteId,
        LocalDate desde,
        LocalDate hasta,
        Integer   facturado,
        int       pagina,
        int       tamano
    ) {
        return repositorio.buscarPorFiltros(
            empresaId,
            (estado     != null && !estado.isBlank())     ? estado     : null,
            (clienteId  != null && !clienteId.isBlank())  ? clienteId  : null,
            desde,
            hasta,
            facturado,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaServicio"))
        );
    }

    /** Uso interno */
    public Servicio getById(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado: " + id));
    }

    /** Uso externo: valida que el servicio pertenezca a la empresa del JWT */
    public Servicio getById(String id, String empresaId) {
        return repositorio.findByIdAndEmpresaIdAndEliminado(id, empresaId, 0)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
    }

    public Servicio crear(String empresaId, ServicioDto datos) {
        String numServicio = (datos.getNumDocumento() != null && !datos.getNumDocumento().isBlank())
            ? datos.getNumDocumento()
            : generarNumero(empresaId);

        BigDecimal valorNeto  = datos.getValorNeto()  != null ? datos.getValorNeto()  : BigDecimal.ZERO;
        BigDecimal iva        = valorNeto.multiply(IVA_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal valorTotal = valorNeto.add(iva);

        Servicio servicio = Servicio.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .clienteId(datos.getClienteId())
            .vehiculoId(datos.getVehiculoId())
            .conductorId(datos.getConductorId())
            .numServicio(generarNumero(empresaId))
            .origen(datos.getOrigen())
            .destino(datos.getDestino())
            .kmsRecorrido(datos.getKmsRecorrido())
            .fechaServicio(datos.getFechaServicio() != null ? datos.getFechaServicio() : LocalDate.now())
            .fechaTermino(datos.getFechaTermino())
            .idaVuelta(datos.getIdaVuelta() != null ? datos.getIdaVuelta() : 0)
            .tipoServicioId(datos.getTipoServicioId())
            .estado(datos.getEstado() != null ? datos.getEstado() : "PENDIENTE")
            .valorNeto(valorNeto)
            .iva(iva)
            .valorTotal(valorTotal)
            .tipoDocumento(datos.getTipoDocumento())
            .numDocumento(datos.getNumDocumento())
            .fechaFactura(datos.getFechaFactura())
            .notas(datos.getNotas())
            .build();

        return repositorio.save(servicio);
    }

    public Servicio actualizar(String id, String empresaId, ServicioDto datos) {
        Servicio s = getById(id, empresaId);

        if (datos.getClienteId()      != null) s.setClienteId(datos.getClienteId());
        if (datos.getVehiculoId()     != null) s.setVehiculoId(datos.getVehiculoId());
        if (datos.getConductorId()    != null) s.setConductorId(datos.getConductorId());
        if (datos.getOrigen()         != null) s.setOrigen(datos.getOrigen());
        if (datos.getDestino()        != null) s.setDestino(datos.getDestino());
        if (datos.getKmsRecorrido()   != null) s.setKmsRecorrido(datos.getKmsRecorrido());
        if (datos.getFechaServicio()  != null) s.setFechaServicio(datos.getFechaServicio());
        if (datos.getFechaTermino()   != null) s.setFechaTermino(datos.getFechaTermino());
        if (datos.getIdaVuelta()      != null) s.setIdaVuelta(datos.getIdaVuelta());
        if (datos.getTipoServicioId() != null) s.setTipoServicioId(datos.getTipoServicioId());
        if (datos.getEstado()         != null) s.setEstado(datos.getEstado());
        if (datos.getTipoDocumento()  != null) s.setTipoDocumento(datos.getTipoDocumento());
        if (datos.getNumDocumento()   != null) s.setNumDocumento(datos.getNumDocumento());
        if (datos.getFechaFactura()   != null) s.setFechaFactura(datos.getFechaFactura());
        if (datos.getNotas()          != null) s.setNotas(datos.getNotas());

        if (datos.getValorNeto() != null) {
            BigDecimal valorNeto  = datos.getValorNeto();
            BigDecimal iva        = valorNeto.multiply(IVA_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal valorTotal = valorNeto.add(iva);
            s.setValorNeto(valorNeto);
            s.setIva(iva);
            s.setValorTotal(valorTotal);
        }

        return repositorio.save(s);
    }

    public Servicio cambiarEstado(String id, String empresaId, String nuevoEstado, List<String> estadosPermitidos) {
        Servicio s = getById(id, empresaId);
        if (!estadosPermitidos.contains(s.getEstado())) {
            throw new IllegalStateException(
                "No se puede cambiar de " + s.getEstado() + " a " + nuevoEstado);
        }
        s.setEstado(nuevoEstado);
        return repositorio.save(s);
    }

    public Servicio asignar(String id, String empresaId, AsignarServicioDto dto) {
        Servicio s = getById(id, empresaId);
        if (!"APROBADO".equals(s.getEstado())) {
            throw new IllegalStateException("Solo se puede asignar cuando el servicio está APROBADO");
        }
        if (dto.getVehiculoId()  != null) s.setVehiculoId(dto.getVehiculoId());
        if (dto.getConductorId() != null) s.setConductorId(dto.getConductorId());
        return repositorio.save(s);
    }

    public List<Servicio> viajesConductor(String conductorId) {
        return repositorio.findByConductorIdAndEliminadoOrderByFechaServicioDesc(conductorId, 0);
    }

    public void eliminar(String id, String empresaId) {
        Servicio s = getById(id, empresaId);
        s.setEliminado(1);
        repositorio.save(s);
    }

    private String generarNumero(String empresaId) {
        String ultimo = repositorio.ultimoNumero(empresaId);
        int siguiente = 1;
        if (ultimo != null && ultimo.contains("-")) {
            try { siguiente = Integer.parseInt(ultimo.split("-")[1]) + 1; } catch (Exception ignored) {}
        }
        return String.format("SRV-%04d", siguiente);
    }
}
