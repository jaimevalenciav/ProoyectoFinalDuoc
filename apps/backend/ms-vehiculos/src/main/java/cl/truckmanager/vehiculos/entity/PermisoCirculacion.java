package cl.truckmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "PERMISOS_CIRCULACION")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermisoCirculacion {

    @Id @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "MUNICIPALIDAD_ID", length = 36)
    private String municipalidadId;

    @Column(name = "FECHA_PAGO")
    private LocalDate fechaPago;

    @Column(name = "VALOR")
    private Long valor;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    @Column(name = "DOCUMENTO", length = 100)
    private String documento;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
