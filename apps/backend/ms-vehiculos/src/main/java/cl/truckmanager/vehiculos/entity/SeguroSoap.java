package cl.truckmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SEGUROS_SOAP")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SeguroSoap {

    @Id @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "ASEGURADORA_ID", length = 36)
    private String aseguradoraId;

    @Column(name = "FECHA_EMISION")
    private LocalDate fechaEmision;

    @Column(name = "VALOR")
    private Long valor;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    @Column(name = "POLIZA", length = 100)
    private String poliza;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
