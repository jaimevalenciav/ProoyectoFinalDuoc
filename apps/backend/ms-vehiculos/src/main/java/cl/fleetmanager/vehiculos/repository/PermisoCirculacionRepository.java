package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.PermisoCirculacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermisoCirculacionRepository extends JpaRepository<PermisoCirculacion, String> {
    List<PermisoCirculacion> findByVehiculoIdOrderByFechaPagoDescCreatedAtDesc(String vehiculoId);
}
