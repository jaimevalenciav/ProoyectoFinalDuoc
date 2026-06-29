package cl.truckmanager.reportes.controller;

import cl.truckmanager.reportes.dto.KpiDashboardDto;
import cl.truckmanager.reportes.entity.UsuarioSistema;
import cl.truckmanager.reportes.repository.UsuarioRepository;
import cl.truckmanager.reportes.service.KpiService;
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
