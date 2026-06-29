package cl.truckmanager.bffmobile.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GpsTrackRequest {
    @NotBlank String idConductor;
    @NotBlank String idVehiculo;
    @NotNull  Double latitud;
    @NotNull  Double longitud;
    Double velocidad;
    Double precision;
    String registradoEn;
}
