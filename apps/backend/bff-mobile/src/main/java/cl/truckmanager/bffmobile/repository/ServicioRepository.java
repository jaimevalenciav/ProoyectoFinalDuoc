package cl.truckmanager.bffmobile.repository;

import cl.truckmanager.bffmobile.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, String> {
    List<Servicio> findByConductorIdAndEstadoIn(String conductorId, List<String> estados);
}
