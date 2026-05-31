package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.Aseguradora;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AseguradoraRepository extends JpaRepository<Aseguradora, String> {
    List<Aseguradora> findByEmpresaIdAndEliminadoOrderByNombreAsc(String empresaId, int eliminado);
}
