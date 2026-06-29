package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.AlertaCombustibleDto;
import cl.truckmanager.operaciones.entity.AlertaCombustible;
import cl.truckmanager.operaciones.repository.AlertaCombustibleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertaCombustibleService {

    private final AlertaCombustibleRepository repo;

    /**
     * Guarda una lista de alertas asociadas a una carga recién registrada.
     * Solo se persisten alertas tipo 'warning' e 'info' (los 'error' bloquean el guardado).
     */
    public void guardar(String empresaId, List<AlertaCombustibleDto> dtos) {
        for (AlertaCombustibleDto dto : dtos) {
            if ("error".equals(dto.getTipo())) continue; // errors bloquean: no se guardan
            AlertaCombustible a = new AlertaCombustible();
            a.setEmpresaId(empresaId);
            a.setCargaId(dto.getCargaId());
            a.setVehiculoId(dto.getVehiculoId());
            a.setTipo(dto.getTipo());
            a.setIcono(dto.getIcono());
            a.setMensaje(dto.getMensaje());
            repo.save(a);
        }
    }

    /**
     * Devuelve alertas de la empresa.
     * @param activas true → solo no leídas; false → solo leídas; null → todas
     */
    @Transactional(readOnly = true)
    public List<AlertaCombustible> getAlertas(String empresaId, Boolean activas) {
        if (activas == null) return repo.findByEmpresaIdOrderByCreatedAtDesc(empresaId);
        return repo.findByEmpresaIdAndLeidaOrderByCreatedAtDesc(empresaId, activas ? 0 : 1);
    }

    /** Marca una alerta como leída y registra quién la confirmó. */
    public AlertaCombustible marcarLeida(String id, String leidaPor) {
        AlertaCombustible a = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + id));
        a.setLeida(1);
        a.setLeidaPor(leidaPor != null && !leidaPor.isBlank() ? leidaPor : "Sistema");
        a.setLeidaAt(LocalDateTime.now());
        return repo.save(a);
    }
}
