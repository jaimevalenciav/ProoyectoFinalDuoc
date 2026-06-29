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
        // 1. Intentar parsear como JSON → extraer campo "id"
        if (qrCode != null && qrCode.trim().startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(qrCode);
                if (node.has("id")) {
                    String vehiculoId = node.get("id").asText();
                    log.info("QR JSON detectado → buscando vehiculo por id: {}", vehiculoId);
                    var porId = vehiculoRepository.findById(vehiculoId);
                    if (porId.isPresent()) return porId.get();
                }
            } catch (Exception e) {
                log.warn("No se pudo parsear QR como JSON: {}", e.getMessage());
            }
        }

        // 2. Buscar por columna QR_CODE (string exacto)
        var porQr = vehiculoRepository.findByQrCode(qrCode);
        if (porQr.isPresent()) return porQr.get();

        // 3. Último intento: tratar el qrCode como vehiculoId directo
        return vehiculoRepository.findById(qrCode)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "QR no válido o vehículo no encontrado: " + qrCode));
    }
}
