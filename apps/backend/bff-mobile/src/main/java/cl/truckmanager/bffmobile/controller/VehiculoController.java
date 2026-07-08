package cl.truckmanager.bffmobile.controller;

import cl.truckmanager.bffmobile.entity.Vehiculo;
import cl.truckmanager.bffmobile.repository.VehiculoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;
    private final ObjectMapper objectMapper;

    /**
     * GET /api/v1/vehiculos/{vehiculoId}
     * Retorna datos básicos del vehículo para la app móvil.
     */
    @GetMapping("/{vehiculoId}")
    public Map<String, Object> obtenerVehiculo(@PathVariable String vehiculoId) {
        log.info("Buscando vehiculo: {}", vehiculoId);

        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Vehiculo no encontrado: " + vehiculoId));

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id",      v.getId());
        m.put("patente", v.getPatente() != null ? v.getPatente() : "");
        m.put("marca",   v.getMarca()   != null ? v.getMarca()   : "");
        m.put("modelo",  v.getModelo()  != null ? v.getModelo()  : "");
        return m;
    }

    /**
     * GET /api/v1/vehiculos/qr/validar?qrCode=...
     * El QR puede ser:
     *  - JSON: {"tipo":"vehiculo","id":"VEH-001","patente":"BGJK-91"} → usa el campo "id"
     *  - String plano: busca directamente por QR_CODE en la tabla, o por ID
     */
    @GetMapping("/qr/validar")
    public Map<String, Object> validarQr(@RequestParam String qrCode) {
        log.info("Validando QR: {}", qrCode);

        Vehiculo v = resolverVehiculoDesdeQr(qrCode);

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("vehiculoId", v.getId());
        m.put("placa",      v.getPatente() != null ? v.getPatente() : "");
        m.put("marca",      v.getMarca()   != null ? v.getMarca()   : "");
        m.put("modelo",     v.getModelo()  != null ? v.getModelo()  : "");
        return m;
    }

    private Vehiculo resolverVehiculoDesdeQr(String qrCode) {
        // 1. Si el QR es JSON, extraer el campo "id"
        String vehiculoId = null;
        if (qrCode != null && qrCode.trim().startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(qrCode);
                if (node.has("id")) {
                    vehiculoId = node.get("id").asText();
                    log.info("QR JSON detectado, vehiculoId extraido: [{}]", vehiculoId);
                } else {
                    log.warn("QR JSON sin campo 'id': {}", qrCode);
                }
            } catch (Exception e) {
                log.warn("QR no es JSON valido: {}", e.getMessage());
            }
        }

        // 2. Buscar por id extraído del JSON
        if (vehiculoId != null && !vehiculoId.isBlank()) {
            try {
                var porId = vehiculoRepository.findById(vehiculoId);
                if (porId.isPresent()) {
                    log.info("Vehiculo encontrado por id: {}", vehiculoId);
                    return porId.get();
                }
                log.warn("findById({}) retorno vacio, buscando por QR_CODE", vehiculoId);
            } catch (Exception e) {
                log.error("Error buscando vehiculo por id [{}]: {}", vehiculoId, e.getMessage());
            }
        }

        // 3. Buscar por columna QR_CODE (string exacto del QR)
        try {
            var porQr = vehiculoRepository.findByQrCode(qrCode);
            if (porQr.isPresent()) {
                log.info("Vehiculo encontrado por QR_CODE");
                return porQr.get();
            }
        } catch (Exception e) {
            log.error("Error buscando por QR_CODE: {}", e.getMessage());
        }

        // 4. Último intento: qrCode como vehiculoId directo
        log.warn("Ultimo intento: buscar vehiculo por id directo [{}]", qrCode);
        return vehiculoRepository.findById(qrCode)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "QR no valido o vehiculo no encontrado"));
    }
}
