package cl.truckmanager.vehiculos.repository;

import cl.truckmanager.vehiculos.entity.SeguroSoap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguroSoapRepository extends JpaRepository<SeguroSoap, String> {
    List<SeguroSoap> findByVehiculoIdOrderByFechaEmisionDescCreatedAtDesc(String vehiculoId);
}
