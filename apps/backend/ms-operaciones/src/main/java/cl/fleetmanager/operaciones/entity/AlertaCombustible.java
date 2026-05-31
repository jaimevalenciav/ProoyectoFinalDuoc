package cl.fleetmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ALERTAS_COMBUSTIBLE")
@Getter @Setter @NoArgsConstructor
public class AlertaCombustible {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "CARGA_ID", length = 36, nullable = false)
    private String cargaId;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    /** 'error' | 'warning' | 'info' */
    @Column(name = "TIPO", length = 10, nullable = false)
    private String tipo;

    @Column(name = "ICONO", length = 50)
    private String icono;

    @Column(name = "MENSAJE", length = 1000, nullable = false)
    private String mensaje;

    @Column(name = "LEIDA", nullable = false)
    private int leida = 0;

    @Column(name = "LEIDA_POR", length = 200)
    private String leidaPor;

    @Column(name = "LEIDA_AT")
    private LocalDateTime leidaAt;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
