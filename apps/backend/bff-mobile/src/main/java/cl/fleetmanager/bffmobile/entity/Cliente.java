package cl.fleetmanager.bffmobile.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CLIENTES")
@Data
@NoArgsConstructor
public class Cliente {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "RAZON_SOCIAL", length = 200)
    private String razonSocial;

    @Column(name = "RUT", length = 20)
    private String rut;
}
