package cl.fleetmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SUCURSALES")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Sucursal {

    @Id @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "NOMBRE", length = 100, nullable = false)
    private String nombre;

    @Column(name = "DIRECCION", length = 200)
    private String direccion;

    @Column(name = "CIUDAD", length = 100)
    private String ciudad;

    @Column(name = "ACTIVA")
    private Integer activa;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activa == null)   activa   = 1;
        if (eliminado == null) eliminado = 0;
        createdAt = LocalDateTime.now();
    }
}
