package cl.truckmanager.bffmobile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerfilResponse {
    private String conductorId;
    private String nombre;
    private String email;
}
