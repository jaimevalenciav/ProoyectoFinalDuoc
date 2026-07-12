package cl.truckmanager.reportes.controller;

import cl.truckmanager.reportes.dto.ConsumoVehiculoDto;
import cl.truckmanager.reportes.dto.CostoMantenimientoDto;
import cl.truckmanager.reportes.dto.KpiDashboardDto;
import cl.truckmanager.reportes.entity.UsuarioSistema;
import cl.truckmanager.reportes.repository.UsuarioRepository;
import cl.truckmanager.reportes.service.KpiService;
import cl.truckmanager.reportes.service.ReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final KpiService kpiService;
    private final ReportesService reportesService;
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
    public KpiDashboardDto getKpis(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        return kpiService.getDashboard(empresaId(jwt), d, h);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reportes/consumo-flota?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
    // -------------------------------------------------------------------------
    @GetMapping("/consumo-flota")
    public List<ConsumoVehiculoDto> getConsumoFlota(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportesService.getConsumoFlota(empresaId(jwt), desde, hasta);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reportes/costos-mantenimiento?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
    // -------------------------------------------------------------------------
    @GetMapping("/costos-mantenimiento")
    public List<CostoMantenimientoDto> getCostosMantenimiento(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportesService.getCostosMantenimiento(empresaId(jwt), desde, hasta);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reportes/exportar?tipo=combustible|mantenimiento|servicios
    //                              &desde=YYYY-MM-DD&hasta=YYYY-MM-DD
    // -------------------------------------------------------------------------
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {

        byte[] excel = reportesService.exportarExcel(empresaId(jwt), tipo, desde, hasta);

        String filename = "reporte-" + tipo + "-" + desde + "-" + hasta + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
