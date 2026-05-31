package cl.fleetmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "MUNICIPALIDADES")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Municipalidad {

    @Id @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "NOMBRE", length = 100, nullable = false)
    private String nombre;

    @Column(name = "REGION", length = 100)
    private String region;

    @Column(name = "ACTIVA")
    private Integer activa;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activa == null)    activa    = 1;
        if (eliminado == null) eliminado = 0;
    }
}
