package cl.truckmanager.vehiculos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsPosicionActualDto {
    private Long    id;
    private String  empresaId;
    private String  vehiculoId;
    private String  conductorId;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal velocidad;
    private BigDecimal rumbo;
    private String  estadoMotor;
    private Long    odometro;
    private LocalDateTime recordedAt;
    // campos enriquecidos del vehículo
    private String  patente;
    private String  marca;
    private String  modelo;
    private String  estadoVehiculo;
    private String  conductorNombre;
}
