package cl.truckmanager.bffmobile.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TIPOS_SERVICIO")
@Data
@NoArgsConstructor
public class TipoServicio {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NOMBRE", length = 200)
    private String nombre;
}
