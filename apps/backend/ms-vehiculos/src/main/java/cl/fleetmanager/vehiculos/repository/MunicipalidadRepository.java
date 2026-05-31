package cl.fleetmanager.vehiculos.repository;

import cl.fleetmanager.vehiculos.entity.Municipalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MunicipalidadRepository extends JpaRepository<Municipalidad, String> {
    List<Municipalidad> findByEmpresaIdAndEliminadoOrderByNombreAsc(String empresaId, int eliminado);
}
