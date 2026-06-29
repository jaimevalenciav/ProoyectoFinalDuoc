package cl.truckmanager.bffmobile.controller;

import cl.truckmanager.bffmobile.entity.Servicio;
import cl.truckmanager.bffmobile.repository.ClienteRepository;
import cl.truckmanager.bffmobile.repository.ServicioRepository;
import cl.truckmanager.bffmobile.repository.TipoServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioRepository     servicioRepository;
    private final ClienteRepository      clienteRepository;
    private final TipoServicioRepository tipoServicioRepository;

    /**
     * GET /api/v1/servicios/conductor/{conductorId}
     * Devuelve los servicios APROBADOS o EN_CURSO del conductor con datos enriquecidos.
     */
    @GetMapping("/conductor/{conductorId}")
    public List<Map<String, Object>> obtenerServiciosConductor(@PathVariable String conductorId) {
        log.info("Buscando servicios para conductor: {}", conductorId);

        List<Servicio> servicios = servicioRepository
                .findByConductorIdAndEstadoIn(conductorId, List.of("APROBADO", "EN_CURSO"));

        log.info("Encontrados {} servicios para conductor {}", servicios.size(), conductorId);

        return servicios.stream().map(this::enriquecer).toList();
    }

    /**
     * PATCH /api/v1/servicios/{servicioId}/iniciar
     * Cambia el estado del servicio a EN_CURSO.
     */
    @PatchMapping("/{servicioId}/iniciar")
    public Map<String, Object> iniciarServicio(@PathVariable String servicioId) {
        log.info("Iniciando servicio: {}", servicioId);

        Servicio s = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Servicio no encontrado: " + servicioId));

        s.setEstado("EN_CURSO");
        servicioRepository.save(s);

        return enriquecer(s);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> enriquecer(Servicio s) {
        // Cliente
        String clienteNombre = "";
        String clienteRut    = "";
        if (s.getClienteId() != null) {
            var cliente = clienteRepository.findById(s.getClienteId());
            if (cliente.isPresent()) {
                clienteNombre = cliente.get().getRazonSocial() != null ? cliente.get().getRazonSocial() : "";
                clienteRut    = cliente.get().getRut()          != null ? cliente.get().getRut()          : "";
            }
        }

        // Tipo de servicio
        String tipoNombre = "";
        if (s.getTipoServicioId() != null) {
            var tipo = tipoServicioRepository.findById(s.getTipoServicioId());
            if (tipo.isPresent() && tipo.get().getNombre() != null)
                tipoNombre = tipo.get().getNombre();
        }

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id",            s.getId());
        m.put("numServicio",   s.getNumServicio()   != null ? s.getNumServicio()   : "");
        m.put("conductorId",   s.getConductorId());
        m.put("vehiculoId",    s.getVehiculoId());
        m.put("origen",        s.getOrigen()        != null ? s.getOrigen()        : "");
        m.put("destino",       s.getDestino()       != null ? s.getDestino()       : "");
        m.put("estado",        s.getEstado()        != null ? s.getEstado()        : "");
        m.put("fechaServicio", s.getFechaServicio()  != null ? s.getFechaServicio().toString() : null);
        m.put("tipoServicio",  tipoNombre);
        m.put("clienteNombre", clienteNombre);
        m.put("clienteRut",    clienteRut);
        return m;
    }
}
