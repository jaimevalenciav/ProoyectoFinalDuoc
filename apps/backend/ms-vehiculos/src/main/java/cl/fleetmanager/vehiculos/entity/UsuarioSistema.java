package cl.fleetmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "USUARIOS_SISTEMA")
@Immutable
@Data
@NoArgsConstructor
public class UsuarioSistema {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "AZURE_OID", length = 100)
    private String azureOid;
}
