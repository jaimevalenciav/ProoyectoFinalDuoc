package cl.fleetmanager.reportes.controller;

import cl.fleetmanager.reportes.dto.KpiDashboardDto;
import cl.fleetmanager.reportes.entity.UsuarioSistema;
import cl.fleetmanager.reportes.repository.UsuarioRepository;
import cl.fleetmanager.reportes.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final KpiService kpiService;
    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
                .map(UsuarioSistema::getEmpresaId)
                .orElse("EMP-001");
    }

    @GetMapping("/kpis")
    public KpiDashboardDto getKpis(@AuthenticationPrincipal Jwt jwt) {
        return kpiService.getDashboard(empresaId(jwt));
    }
}
