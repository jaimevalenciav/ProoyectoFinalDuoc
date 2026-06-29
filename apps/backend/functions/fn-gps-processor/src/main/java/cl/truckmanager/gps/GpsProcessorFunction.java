package cl.truckmanager.gps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class GpsProcessorFunction {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final AtomicBoolean WALLET_LISTO = new AtomicBoolean(false);
    private static final String WALLET_DIR = "/tmp/oracle-wallet";

    @FunctionName("ProcesadorGps")
    public void ejecutar(
        @ServiceBusQueueTrigger(name = "mensaje", queueName = "pistas-gps", connection = "BUS_SERVICIO_CONEXION") String mensaje,
        final ExecutionContext contexto
    ) {
        Logger registro = contexto.getLogger();
        registro.info("Procesando mensaje de pista GPS");

        try {
            prepararWallet(registro);
            MensajePistaGps pista = MAPEADOR.readValue(mensaje, MensajePistaGps.class);
            persistirPista(pista, registro);
        } catch (Exception e) {
            registro.severe("Error al procesar mensaje GPS: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** Extrae los archivos del wallet desde el JAR a /tmp/oracle-wallet (solo la primera vez). */
    private void prepararWallet(Logger registro) throws Exception {
        if (WALLET_LISTO.get()) return;

        synchronized (WALLET_LISTO) {
            if (WALLET_LISTO.get()) return;

            Path dir = Paths.get(WALLET_DIR);
            Files.createDirectories(dir);

            String[] archivos = {"cwallet.sso", "tnsnames.ora", "sqlnet.ora", "ewallet.p12"};
            for (String archivo : archivos) {
                try (InputStream is = getClass().getResourceAsStream("/wallet/" + archivo)) {
                    if (is != null) {
                        Files.copy(is, dir.resolve(archivo), StandardCopyOption.REPLACE_EXISTING);
                        registro.info("Wallet extraído: " + archivo);
                    } else {
                        registro.warning("No se encontró en el JAR: " + archivo);
                    }
                }
            }

            // Apuntar Oracle al directorio del wallet
            System.setProperty("oracle.net.tns_admin", WALLET_DIR);
            WALLET_LISTO.set(true);
            registro.info("Wallet Oracle listo en " + WALLET_DIR);
        }
    }

    private void persistirPista(MensajePistaGps pista, Logger registro) throws Exception {
        String tnsAlias = System.getenv("ORACLE_TNS_ALIAS");
        String urlBd;
        if (tnsAlias != null && !tnsAlias.isBlank()) {
            urlBd = "jdbc:oracle:thin:@" + tnsAlias;
        } else {
            urlBd = System.getenv("ORACLE_JDBC_URL");
        }
        String usuario = System.getenv("ORACLE_USUARIO");
        String clave   = System.getenv("ORACLE_CLAVE");

        Timestamp ts = pista.registradoEn() != null
            ? Timestamp.valueOf(pista.registradoEn().replace("T", " ").replace("Z", "").substring(0, 19))
            : new Timestamp(System.currentTimeMillis());

        // Pasar wallet location explícitamente como propiedad JDBC
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", usuario);
        props.setProperty("password", clave);
        props.setProperty("oracle.net.tns_admin", WALLET_DIR);
        props.setProperty("oracle.net.wallet_location",
            "(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=" + WALLET_DIR + ")))");
        registro.info("Conectando a Oracle: url=" + urlBd + " tnsAdmin=" + WALLET_DIR);

        try (Connection conexion = DriverManager.getConnection(urlBd, props)) {
            conexion.setAutoCommit(false);

            // ── 1. Insertar en PISTAS_GPS (tabla original) ─────────────────
            String sqlPistas = "INSERT INTO PISTAS_GPS "
                + "(ID, ID_VEHICULO, ID_CONDUCTOR, LATITUD, LONGITUD, VELOCIDAD, PRECISION_M, REGISTRADO_EN) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conexion.prepareStatement(sqlPistas)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, pista.idVehiculo());
                ps.setString(3, pista.idConductor());
                ps.setDouble(4, pista.latitud());
                ps.setDouble(5, pista.longitud());
                if (pista.velocidad() != null) ps.setDouble(6, pista.velocidad()); else ps.setNull(6, Types.DOUBLE);
                if (pista.precision() != null) ps.setDouble(7, pista.precision()); else ps.setNull(7, Types.DOUBLE);
                ps.setTimestamp(8, ts);
                ps.executeUpdate();
                registro.info("PISTAS_GPS OK — vehiculo=" + pista.idVehiculo());
            }

            // ── 2. Insertar en GPS_TRACKS (tabla que lee el mapa web) ──────
            // Obtener EMPRESA_ID desde VEHICULOS
            String empresaId = null;
            String sqlEmpresa = "SELECT EMPRESA_ID FROM VEHICULOS WHERE ID = ?";
            try (PreparedStatement ps = conexion.prepareStatement(sqlEmpresa)) {
                ps.setString(1, pista.idVehiculo());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) empresaId = rs.getString("EMPRESA_ID");
            }

            if (empresaId != null) {
                // GPS_TRACKS usa identity column para ID — no lo incluimos
                String sqlTracks = "INSERT INTO GPS_TRACKS "
                    + "(EMPRESA_ID, VEHICULO_ID, CONDUCTOR_ID, LATITUD, LONGITUD, VELOCIDAD, PRECISION_M) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conexion.prepareStatement(sqlTracks)) {
                    ps.setString(1, empresaId);
                    ps.setString(2, pista.idVehiculo());
                    ps.setString(3, pista.idConductor());
                    ps.setBigDecimal(4, new java.math.BigDecimal(pista.latitud()).setScale(7, java.math.RoundingMode.HALF_UP));
                    ps.setBigDecimal(5, new java.math.BigDecimal(pista.longitud()).setScale(7, java.math.RoundingMode.HALF_UP));
                    if (pista.velocidad() != null)
                        ps.setBigDecimal(6, new java.math.BigDecimal(pista.velocidad() * 3.6).setScale(1, java.math.RoundingMode.HALF_UP)); // m/s → km/h
                    else
                        ps.setNull(6, Types.DECIMAL);
                    if (pista.precision() != null)
                        ps.setBigDecimal(7, new java.math.BigDecimal(pista.precision()).setScale(1, java.math.RoundingMode.HALF_UP));
                    else
                        ps.setNull(7, Types.DECIMAL);
                    ps.executeUpdate();
                    registro.info("GPS_TRACKS OK — vehiculo=" + pista.idVehiculo() + " empresa=" + empresaId);
                }
            } else {
                registro.warning("Vehiculo no encontrado en VEHICULOS — solo se guarda en PISTAS_GPS: " + pista.idVehiculo());
            }

            conexion.commit();
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
