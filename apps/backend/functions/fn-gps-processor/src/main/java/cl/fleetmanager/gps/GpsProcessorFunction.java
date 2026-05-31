package cl.fleetmanager.gps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class GpsProcessorFunction {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

    @FunctionName("ProcesadorGps")
    public void ejecutar(
        @ServiceBusQueueTrigger(name = "mensaje", queueName = "pistas-gps", connection = "BUS_SERVICIO_CONEXION") String mensaje,
        final ExecutionContext contexto
    ) {
        Logger registro = contexto.getLogger();
        registro.info("Procesando mensaje de pista GPS");

        try {
            MensajePistaGps pista = MAPEADOR.readValue(mensaje, MensajePistaGps.class);
            persistirPista(pista, registro);
        } catch (Exception e) {
            registro.severe("Error al procesar mensaje GPS: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void persistirPista(MensajePistaGps pista, Logger registro) throws Exception {
        String urlBd   = System.getenv("ORACLE_JDBC_URL");
        String usuario = System.getenv("ORACLE_USUARIO");
        String clave   = System.getenv("ORACLE_CLAVE");

        String sql = "INSERT INTO PISTAS_GPS (ID, ID_VEHICULO, ID_CONDUCTOR, LATITUD, LONGITUD, VELOCIDAD, PRECISION_M, REGISTRADO_EN) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = DriverManager.getConnection(urlBd, usuario, clave);
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, pista.idVehiculo());
            ps.setString(3, pista.idConductor());
            ps.setDouble(4, pista.latitud());
            ps.setDouble(5, pista.longitud());
            if (pista.velocidad()  != null) ps.setDouble(6, pista.velocidad());  else ps.setNull(6, Types.DOUBLE);
            if (pista.precision()  != null) ps.setDouble(7, pista.precision());  else ps.setNull(7, Types.DOUBLE);
            ps.setTimestamp(8, pista.registradoEn() != null
                ? Timestamp.valueOf(pista.registradoEn().replace("T", " ").replace("Z", ""))
                : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            registro.info("Pista GPS persistida para vehículo " + pista.idVehiculo());
        }
    }

    public record MensajePistaGps(
        String idConductor,
        String idVehiculo,
        Double latitud,
        Double longitud,
        Double velocidad,
        Double precision,
        String registradoEn
    ) {}
}
