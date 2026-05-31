package cl.fleetmanager.taller.repository;

import cl.fleetmanager.taller.entity.TareaOT;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TareaOTRepository extends JpaRepository<TareaOT, String> {
    List<TareaOT> findByOrdenTrabajoIdOrderByOrdenAsc(String otId);
}
