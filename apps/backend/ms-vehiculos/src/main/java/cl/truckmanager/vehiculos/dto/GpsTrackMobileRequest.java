package cl.truckmanager.vehiculos.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Payload enviado por la app móvil al reportar ubicación GPS.
 * Ruta: POST /api/v1/mobile/gps/track
 */
@Data
public class GpsTrackMobileRequest {
    private String     conductorId;
    private String     vehiculoId;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal velocidad;
    private BigDecimal precision;
    /** ISO-8601, ej: "2025-06-14T10:30:00Z" — se ignora; Oracle usa SYSTIMESTAMP */
    private String     recordedAt;
}
