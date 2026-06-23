package cl.fleetmanager.bffmobile.repository;

import cl.fleetmanager.bffmobile.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {
    java.util.Optional<Vehiculo> findByQrCode(String qrCode);
}
