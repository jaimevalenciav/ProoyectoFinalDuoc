package cl.fleetmanager.bffmobile.repository;

import cl.fleetmanager.bffmobile.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, String> {
    List<Servicio> findByConductorIdAndEstadoIn(String conductorId, List<String> estados);
}
