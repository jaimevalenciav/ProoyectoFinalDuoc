package cl.fleetmanager.bffmobile.controller;

import cl.fleetmanager.bffmobile.dto.GpsTrackRequest;
import cl.fleetmanager.bffmobile.service.GpsPublisherService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/movil/gps")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MobileGpsController {

    private final GpsPublisherService publicador;

    @PostMapping("/pista")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> registrarPista(@Valid @RequestBody GpsTrackRequest solicitud) {
        publicador.publicar(solicitud);
        return Map.of("estado", "aceptado");
    }
}
