package cl.fleetmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "VEHICULOS")
@Immutable
@Data
@NoArgsConstructor
public class VehiculoKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "ESTADO", length = 30)
    private String estado;
}
