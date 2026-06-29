package cl.truckmanager.conductores.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta del endpoint GET /perfil-actual */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDto {
    private String  id;
    private String  nombre;
    private String  email;
    private String  rol;
    private String  empresaId;
    private Integer activo;
}
