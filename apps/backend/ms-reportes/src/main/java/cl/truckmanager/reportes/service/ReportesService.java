package cl.truckmanager.reportes.service;

import cl.truckmanager.reportes.dto.ConsumoVehiculoDto;
import cl.truckmanager.reportes.dto.CostoMantenimientoDto;
import cl.truckmanager.reportes.repository.CargaCombustibleKpiRepository;
import cl.truckmanager.reportes.repository.OrdenTrabajoReportesRepository;
import cl.truckmanager.reportes.repository.ServicioKpiRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportesService {

    private final CargaCombustibleKpiRepository combustibleRepo;
    private final OrdenTrabajoReportesRepository otRepo;
    private final ServicioKpiRepository servicioRepo;

    // -------------------------------------------------------------------------
    // 1. Consumo de flota
    // -------------------------------------------------------------------------

    public List<ConsumoVehiculoDto> getConsumoFlota(String empresaId,
                                                     LocalDate desde,
                                                     LocalDate hasta) {
        return combustibleRepo.findConsumoAgrupado(empresaId, desde, hasta)
                .stream()
                .map(row -> new ConsumoVehiculoDto(
                        str(row[0]),                   // vehiculoId
                        str(row[1]),                   // placa (PATENTE)
                        decimal(row[2]),               // litrosTotales
                        longVal(row[3]),               // kmTotales
                        decimal(row[4]),               // rendimientoPromedio
                        decimal(row[5])                // costoTotal
                ))
                .toList();
    }

    // -------------------------------------------------------------------------
    // 2. Costos de mantenimiento
    // -------------------------------------------------------------------------

    public List<CostoMantenimientoDto> getCostosMantenimiento(String empresaId,
                                                               LocalDate desde,
                                                               LocalDate hasta) {
        return otRepo.findCostosMantenimientoPorMes(empresaId, desde, hasta)
                .stream()
                .map(row -> new CostoMantenimientoDto(
                        str(row[0]),      // mes YYYY-MM
                        decimal(row[1]),  // costoManoObra
                        decimal(row[2]),  // costoRepuestos
                        decimal(row[3])   // total
                ))
                .toList();
    }

    // -------------------------------------------------------------------------
    // 3. Exportar Excel
    // -------------------------------------------------------------------------

    public byte[] exportarExcel(String empresaId, String tipo,
                                 LocalDate desde, LocalDate hasta) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (tipo.toLowerCase()) {
                case "combustible" -> buildHojaCombustible(wb, empresaId, desde, hasta);
                case "mantenimiento" -> buildHojaMantenimiento(wb, empresaId, desde, hasta);
                case "servicios" -> buildHojaServicios(wb, empresaId, desde, hasta);
                default -> throw new IllegalArgumentException("Tipo no soportado: " + tipo);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ---- Hojas Excel --------------------------------------------------------

    private void buildHojaCombustible(Workbook wb, String empresaId,
                                       LocalDate desde, LocalDate hasta) {
        Sheet sheet = wb.createSheet("Combustible");
        String[] headers = {
                "ID", "Vehículo ID", "Patente", "Fecha Carga",
                "Litros", "Precio x Litro", "Costo Total",
                "KM Vehículo", "Tipo Combustible", "Proveedor"
        };
        createHeaderRow(wb, sheet, headers);

        List<Object[]> rows = combustibleRepo.findCargasParaExport(empresaId, desde, hasta);
        int rowIdx = 1;
        for (Object[] r : rows) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < r.length; i++) {
                row.createCell(i).setCellValue(r[i] == null ? "" : r[i].toString());
            }
        }
        autoSize(sheet, headers.length);
    }

    private void buildHojaMantenimiento(Workbook wb, String empresaId,
                                         LocalDate desde, LocalDate hasta) {
        Sheet sheet = wb.createSheet("Mantenimiento");
        String[] headers = {
                "N° OT", "Vehículo ID", "Tipo", "Estado",
                "Fecha Apertura", "Fecha Cierre Real",
                "Costo Mano Obra", "Costo Repuestos", "Costo Total"
        };
        createHeaderRow(wb, sheet, headers);

        List<Object[]> rows = otRepo.findOTsParaExport(empresaId, desde, hasta);
        int rowIdx = 1;
        for (Object[] r : rows) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < r.length; i++) {
                row.createCell(i).setCellValue(r[i] == null ? "" : r[i].toString());
            }
        }
        autoSize(sheet, headers.length);
    }

    private void buildHojaServicios(Workbook wb, String empresaId,
                                     LocalDate desde, LocalDate hasta) {
        Sheet sheet = wb.createSheet("Servicios");
        String[] headers = {
                "N° Servicio", "Vehículo ID", "Origen", "Destino",
                "KMs Recorrido", "Fecha Servicio", "Estado",
                "Valor Neto", "Valor Total"
        };
        createHeaderRow(wb, sheet, headers);

        List<Object[]> rows = servicioRepo.findServiciosParaExport(empresaId, desde, hasta);
        int rowIdx = 1;
        for (Object[] r : rows) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < r.length; i++) {
                row.createCell(i).setCellValue(r[i] == null ? "" : r[i].toString());
            }
        }
        autoSize(sheet, headers.length);
    }

    // ---- Helpers ------------------------------------------------------------

    private void createHeaderRow(Workbook wb, Sheet sheet, String[] headers) {
        CellStyle headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private BigDecimal decimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private Long longVal(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}
