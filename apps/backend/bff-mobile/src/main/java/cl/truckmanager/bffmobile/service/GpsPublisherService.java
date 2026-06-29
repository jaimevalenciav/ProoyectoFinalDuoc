package cl.truckmanager.bffmobile.service;

import cl.truckmanager.bffmobile.dto.GpsTrackRequest;
import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GpsPublisherService {

    @Value("${azure.bus-servicio.cadena-conexion}")
    private String cadenaConexion;

    @Value("${azure.bus-servicio.nombre-cola}")
    private String nombreCola;

    private final ObjectMapper mapeadorJson;

    public void publicar(GpsTrackRequest solicitud) {
        try (ServiceBusSenderClient emisor = new ServiceBusClientBuilder()
                .connectionString(cadenaConexion)
                .sender()
                .queueName(nombreCola)
                .buildClient()) {

            String json = mapeadorJson.writeValueAsString(solicitud);
            emisor.sendMessage(new ServiceBusMessage(json));
            log.debug("Pista GPS publicada para vehículo {}", solicitud.getIdVehiculo());
        } catch (Exception e) {
            log.error("Error al publicar pista GPS: {}", e.getMessage());
        }
    }
}
