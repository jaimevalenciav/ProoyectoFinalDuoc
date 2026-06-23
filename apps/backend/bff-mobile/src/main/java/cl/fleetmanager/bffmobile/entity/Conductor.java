package cl.fleetmanager.bffmobile.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CONDUCTORES")
@Data
@NoArgsConstructor
public class Conductor {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NOMBRE", length = 200)
    private String nombre;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "AZURE_OID", length = 36)
    private String azureOid;

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "ELIMINADO")
    private Integer eliminado;
}
