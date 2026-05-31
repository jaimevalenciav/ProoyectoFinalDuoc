package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.SeguroSoap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguroSoapRepository extends JpaRepository<SeguroSoap, String> {
    List<SeguroSoap> findByVehiculoIdOrderByFechaEmisionDescCreatedAtDesc(String vehiculoId);
}
