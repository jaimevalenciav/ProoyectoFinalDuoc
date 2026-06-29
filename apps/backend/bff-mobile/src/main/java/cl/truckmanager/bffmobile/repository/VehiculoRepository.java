package cl.truckmanager.bffmobile.repository;

import cl.truckmanager.bffmobile.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {
    java.util.Optional<Vehiculo> findByQrCode(String qrCode);
}
