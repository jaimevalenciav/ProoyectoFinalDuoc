package cl.truckmanager.vehiculos.controller;

import cl.truckmanager.vehiculos.dto.VehiculoDto;
import cl.truckmanager.vehiculos.entity.Vehiculo;
import cl.truckmanager.vehiculos.repository.UsuarioRepository;
import cl.truckmanager.vehiculos.service.VehiculoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehiculos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VehiculoController {

    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(u -> u.getEmpresaId())
            .orElse("EMP-001");
    }

    private final VehiculoService servicio;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Page<Vehiculo> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String busqueda,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.obtenerTodos(empresaId(jwt), estado, busqueda, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Vehiculo obtenerPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return servicio.obtenerPorId(id, empresaId(jwt));
    }

    @GetMapping("/qr/{codigoQr}")
    public Vehiculo obtenerPorQr(@PathVariable String codigoQr) {
        return servicio.obtenerPorQr(codigoQr);
    }

    /**
     * Endpoint para la app móvil: valida el QR y devuelve {vehiculoId, placa, marca, modelo}.
     * Soporta dos formatos:
     *   - JSON: {"tipo":"vehiculo","id":"<uuid>","patente":"<patente>"} (generado por el frontend)
     *   - Texto plano: el valor del campo qrCode almacenado en BD
     */
    @GetMapping("/qr/validar")
    public Map<String, String> validarQrMobile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String qrCode) {
        Vehiculo v;
        try {
            JsonNode nodo = objectMapper.readTree(qrCode);
            JsonNode idNodo = nodo.get("id");
            if (idNodo != null && !idNodo.isNull()) {
                // QR en formato JSON — buscar por ID validando empresa
                v = servicio.obtenerPorId(idNodo.asText(), empresaId(jwt));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR sin campo 'id'");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // No es JSON — buscar por qrCode textual
            v = servicio.obtenerPorQr(qrCode);
        }
        return Map.of(
            "vehiculoId", v.getId(),
            "placa",      v.getPatente() != null ? v.getPatente() : "",
            "marca",      v.getMarca()   != null ? v.getMarca()   : "",
            "modelo",     v.getModelo()  != null ? v.getModelo()  : ""
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehiculo crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VehiculoDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public Vehiculo actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody VehiculoDto datos) {
        return servicio.actualizar(id, empresaId(jwt), datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        servicio.eliminar(id, empresaId(jwt));
    }
}
