package cl.fleetmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GPS_TRACKS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GpsTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "CONDUCTOR_ID", length = 36)
    private String conductorId;

    @Column(name = "LATITUD", precision = 11, scale = 7, nullable = false)
    private BigDecimal latitud;

    @Column(name = "LONGITUD", precision = 11, scale = 7, nullable = false)
    private BigDecimal longitud;

    @Column(name = "VELOCIDAD", precision = 5, scale = 1)
    private BigDecimal velocidad;

    @Column(name = "RUMBO", precision = 5, scale = 1)
    private BigDecimal rumbo;

    @Column(name = "PRECISION_M", precision = 6, scale = 1)
    private BigDecimal precisionM;

    @Column(name = "ESTADO_MOTOR", length = 10)
    private String estadoMotor;

    @Column(name = "ODOMETRO")
    private Long odometro;

    @Column(name = "RECORDED_AT", insertable = false, updatable = false)
    private LocalDateTime recordedAt;
}
