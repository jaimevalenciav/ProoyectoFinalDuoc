package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, String> {
    List<Sucursal> findByEmpresaIdAndEliminadoOrderByNombreAsc(String empresaId, int eliminado);
}
