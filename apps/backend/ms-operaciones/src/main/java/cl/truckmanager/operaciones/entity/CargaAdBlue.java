package cl.truckmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CARGAS_ADBLUE")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CargaAdBlue {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "CONDUCTOR_ID", length = 36)
    private String conductorId;

    @Column(name = "NUM_DOCUMENTO", length = 50)
    private String numDocumento;

    @Column(name = "PROVEEDOR", length = 100, nullable = false)
    private String proveedor;

    @Column(name = "ESTACION", length = 200)
    private String estacion;

    @Column(name = "FECHA_CARGA", nullable = false)
    private LocalDate fechaCarga;

    @Column(name = "LITROS", precision = 10, scale = 2, nullable = false)
    private BigDecimal litros;

    @Column(name = "PRECIO_LITRO", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioLitro;

    @Column(name = "COSTO_TOTAL", precision = 14, scale = 2, nullable = false)
    private BigDecimal costoTotal;

    @Column(name = "KM_VEHICULO", nullable = false)
    private Long kmVehiculo;

    /** % AdBlue vs diésel consumido en el período (informativo) */
    @Column(name = "PCT_DIESEL", precision = 6, scale = 2)
    private BigDecimal pctDiesel;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
