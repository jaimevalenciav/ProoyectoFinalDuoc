package cl.fleetmanager.operaciones.service;

import cl.fleetmanager.operaciones.entity.Cliente;
import cl.fleetmanager.operaciones.entity.Factura;
import cl.fleetmanager.operaciones.entity.Servicio;
import cl.fleetmanager.operaciones.repository.ClienteRepository;
import cl.fleetmanager.operaciones.repository.FacturaRepository;
import cl.fleetmanager.operaciones.repository.ServicioRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturaPdfService {

    private final FacturaRepository  facturaRepo;
    private final ClienteRepository  clienteRepo;
    private final ServicioRepository servicioRepo;

    // ── Fuentes (todas en negro) ──────────────────────────────────────────────
    private static final Font F_EMPRESA   = new Font(Font.HELVETICA, 14, Font.BOLD,   Color.BLACK);
    private static final Font F_GIRO      = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.BLACK);
    private static final Font F_NORMAL    = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.BLACK);
    private static final Font F_BOLD      = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.BLACK);
    private static final Font F_SMALL     = new Font(Font.HELVETICA,  8, Font.NORMAL, Color.BLACK);
    private static final Font F_SMALL_B   = new Font(Font.HELVETICA,  8, Font.BOLD,   Color.BLACK);
    private static final Font F_BOX_TITLE = new Font(Font.HELVETICA, 11, Font.BOLD,   Color.BLACK);
    private static final Font F_BOX_NUM   = new Font(Font.HELVETICA, 14, Font.BOLD,   Color.BLACK);
    private static final Font F_BOX_RUT   = new Font(Font.HELVETICA, 10, Font.BOLD,   Color.BLACK);
    private static final Font F_HEAD_TAB  = new Font(Font.HELVETICA,  8, Font.BOLD,   Color.BLACK);
    private static final Font F_TOTAL_LBL = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.BLACK);
    private static final Font F_TOTAL_VAL = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.BLACK);

    private static final NumberFormat CLP = NumberFormat.getNumberInstance(new Locale("es", "CL"));
    static { CLP.setMaximumFractionDigits(0); CLP.setMinimumFractionDigits(0); }

    // ── Datos del emisor (hardcoded para el proyecto) ─────────────────────────
    private static final String EMISOR_NOMBRE  = "TRUCKMANAGER PRO S.A.";
    private static final String EMISOR_GIRO    = "SERVICIOS DE TRANSPORTE DE CARGA Y LOGÍSTICA";
    private static final String EMISOR_DIRECCION = "AV. APOQUINDO 4501, PISO 12";
    private static final String EMISOR_CIUDAD  = "LAS CONDES, SANTIAGO";
    private static final String EMISOR_RUT     = "76.543.210-8";
    private static final String EMISOR_SII     = "S.I.I. - SANTIAGO ORIENTE";

    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generar(String facturaId) throws Exception {
        Factura factura = facturaRepo.findById(facturaId)
            .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + facturaId));

        Cliente cliente = clienteRepo.findById(factura.getClienteId()).orElse(null);
        List<Servicio> servicios = servicioRepo.findByFacturaId(facturaId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 45, 45);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Secciones ──────────────────────────────────────────────────────
        doc.add(cabeceraEmisor(factura));
        doc.add(espacio(6f));
        doc.add(lineaHorizontal());
        doc.add(espacio(5f));
        doc.add(datosReceptor(cliente));
        doc.add(espacio(6f));
        if (factura.getNotas() != null && !factura.getNotas().isBlank()) {
            doc.add(lineaReferencia(factura.getNotas()));
            doc.add(espacio(6f));
        }
        doc.add(tablaServicios(servicios));
        doc.add(espacio(10f));
        doc.add(pie(factura));

        doc.close();
        return out.toByteArray();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. CABECERA: datos emisor (izq) + box folio (der)
    // ═════════════════════════════════════════════════════════════════════════
    private PdfPTable cabeceraEmisor(Factura f) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{60, 40});

        // ── Columna izquierda: emisor ──────────────────────────────────────
        PdfPCell cEmisor = new PdfPCell();
        cEmisor.setBorder(Rectangle.NO_BORDER);
        cEmisor.setPaddingRight(12);

        Paragraph pEmpresa = new Paragraph(EMISOR_NOMBRE, F_EMPRESA);
        pEmpresa.setSpacingAfter(3);
        cEmisor.addElement(pEmpresa);

        cEmisor.addElement(new Paragraph(EMISOR_GIRO, F_GIRO));
        cEmisor.addElement(espacio(10f));
        cEmisor.addElement(new Paragraph(EMISOR_DIRECCION, F_NORMAL));
        cEmisor.addElement(new Paragraph(EMISOR_CIUDAD, F_NORMAL));

        t.addCell(cEmisor);

        // ── Columna derecha: box folio con borde doble ────────────────────
        PdfPCell cBox = celdaBox(f);
        t.addCell(cBox);

        return t;
    }

    private PdfPCell celdaBox(Factura f) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        // RUT
        PdfPCell cRut = new PdfPCell(new Phrase("R.U.T.: " + EMISOR_RUT, F_BOX_RUT));
        cRut.setHorizontalAlignment(Element.ALIGN_CENTER);
        cRut.setPadding(6);
        cRut.setBorderWidthBottom(0.5f);
        cRut.setBorderWidthTop(0);
        cRut.setBorderWidthLeft(0);
        cRut.setBorderWidthRight(0);
        cRut.setBorderColor(Color.BLACK);
        inner.addCell(cRut);

        // FACTURA ELECTRONICA
        PdfPCell cTipo = new PdfPCell(new Phrase("FACTURA ELECTRÓNICA", F_BOX_TITLE));
        cTipo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTipo.setPadding(7);
        cTipo.setBorderWidthBottom(0.5f);
        cTipo.setBorderWidthTop(0);
        cTipo.setBorderWidthLeft(0);
        cTipo.setBorderWidthRight(0);
        cTipo.setBorderColor(Color.BLACK);
        inner.addCell(cTipo);

        // N°
        // Mostrar solo el número (ej: "FAC-0004" → "0004")
        String rawNum = f.getNumFactura() != null ? f.getNumFactura() : "—";
        String num = rawNum.contains("-") ? rawNum.substring(rawNum.lastIndexOf('-') + 1) : rawNum;
        PdfPCell cNum = new PdfPCell(new Phrase("N° " + num, F_BOX_NUM));
        cNum.setHorizontalAlignment(Element.ALIGN_CENTER);
        cNum.setPadding(7);
        cNum.setBorderWidthBottom(0.5f);
        cNum.setBorderWidthTop(0);
        cNum.setBorderWidthLeft(0);
        cNum.setBorderWidthRight(0);
        cNum.setBorderColor(Color.BLACK);
        inner.addCell(cNum);

        // SII
        PdfPCell cSii = new PdfPCell(new Phrase(EMISOR_SII, F_BOLD));
        cSii.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSii.setPaddingTop(6);
        cSii.setPaddingBottom(6);
        cSii.setBorder(Rectangle.NO_BORDER);
        inner.addCell(cSii);

        // Celda exterior con borde grueso
        PdfPCell exterior = new PdfPCell(inner);
        exterior.setPadding(0);
        exterior.setBorderWidth(2f);
        exterior.setBorderColor(Color.BLACK);
        return exterior;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. DATOS DEL RECEPTOR
    // ═════════════════════════════════════════════════════════════════════════
    private PdfPTable datosReceptor(Cliente c) throws Exception {
        // Fecha en texto
        LocalDate hoy = LocalDate.now();
        String fechaTexto = "Santiago, " + hoy.getDayOfMonth() + " de " +
            mesEnLetras(hoy.getMonthValue()) + " de " + hoy.getYear();

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        // Fecha
        PdfPCell cFecha = new PdfPCell(new Phrase(fechaTexto.toUpperCase(), F_NORMAL));
        cFecha.setBorder(Rectangle.NO_BORDER);
        cFecha.setPaddingBottom(8);
        t.addCell(cFecha);

        // Filas de receptor (tabla 2 columnas: etiqueta | valor)
        PdfPTable tReceptor = new PdfPTable(2);
        tReceptor.setWidthPercentage(100);
        tReceptor.setWidths(new float[]{18, 82});

        String nombre    = c != null && c.getRazonSocial() != null ? c.getRazonSocial().toUpperCase() : "—";
        String rut       = c != null && c.getRut()         != null ? c.getRut()        : "—";
        String giro      = c != null && c.getGiro()        != null ? c.getGiro().toUpperCase() : "—";
        String email     = c != null && c.getEmail()       != null ? c.getEmail()      : "";
        String ciudad    = c != null && c.getCiudad()      != null ? c.getCiudad().toUpperCase() : "—";

        agregarFilaReceptor(tReceptor, "SEÑOR (ES):", nombre);
        agregarFilaReceptor(tReceptor, "R.U.T.:",     rut);
        agregarFilaReceptor(tReceptor, "GIRO:",        giro);
        if (!email.isBlank())
            agregarFilaReceptor(tReceptor, "E-MAIL:",  email);
        agregarFilaReceptor(tReceptor, "CIUDAD:",      ciudad);

        PdfPCell cReceptor = new PdfPCell(tReceptor);
        cReceptor.setBorder(Rectangle.NO_BORDER);
        cReceptor.setPadding(0);
        t.addCell(cReceptor);

        return t;
    }

    private void agregarFilaReceptor(PdfPTable t, String label, String valor) {
        PdfPCell cL = new PdfPCell(new Phrase(label, F_BOLD));
        cL.setBorder(Rectangle.NO_BORDER);
        cL.setPaddingBottom(2);
        t.addCell(cL);

        PdfPCell cV = new PdfPCell(new Phrase(valor, F_NORMAL));
        cV.setBorder(Rectangle.NO_BORDER);
        cV.setPaddingBottom(2);
        t.addCell(cV);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. LÍNEA DE REFERENCIA (notas)
    // ═════════════════════════════════════════════════════════════════════════
    private Paragraph lineaReferencia(String notas) {
        Paragraph p = new Paragraph();
        p.add(new Chunk("Referencia: ", F_BOLD));
        p.add(new Chunk(notas, F_NORMAL));
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. TABLA DE SERVICIOS
    // ═════════════════════════════════════════════════════════════════════════
    private PdfPTable tablaServicios(List<Servicio> servicios) throws Exception {
        PdfPTable t = new PdfPTable(6);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{6, 12, 42, 9, 16, 15});

        // Encabezados
        String[] headers = {"Ítem", "Código", "Detalle", "Cantidad", "P. Unitario", "Total"};
        int[] aligns = {
            Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_LEFT,
            Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell th = new PdfPCell(new Phrase(headers[i], F_HEAD_TAB));
            th.setHorizontalAlignment(aligns[i]);
            th.setPadding(5);
            th.setBorderWidth(0.5f);
            th.setBorderColor(Color.BLACK);
            th.setBackgroundColor(Color.WHITE);
            t.addCell(th);
        }

        // Filas de servicios
        int item = 1;
        for (Servicio s : servicios) {
            String detalle = construirDetalle(s);
            String pUnit   = "$ " + CLP.format(s.getValorNeto() != null ? s.getValorNeto() : BigDecimal.ZERO);
            String total   = "$ " + CLP.format(s.getValorTotal() != null ? s.getValorTotal() : BigDecimal.ZERO);

            agregarCeldaTabla(t, String.valueOf(item++), Element.ALIGN_CENTER);
            agregarCeldaTabla(t, safe(s.getNumServicio()),               Element.ALIGN_CENTER);
            agregarCeldaDetalle(t, detalle);
            agregarCeldaTabla(t, "1",                                    Element.ALIGN_CENTER);
            agregarCeldaTabla(t, pUnit,                                  Element.ALIGN_RIGHT);
            agregarCeldaTabla(t, total,                                  Element.ALIGN_RIGHT);
        }

        // Filas vacías para dar espacio visual (mínimo 8 filas)
        int filasMostradas = servicios.size();
        int filasVacias = Math.max(0, 8 - filasMostradas);
        for (int i = 0; i < filasVacias; i++) {
            for (int j = 0; j < 6; j++) {
                PdfPCell empty = new PdfPCell(new Phrase(" ", F_NORMAL));
                empty.setPadding(5);
                empty.setBorderWidth(0.5f);
                empty.setBorderWidthTop(0);
                empty.setBorderColor(Color.BLACK);
                t.addCell(empty);
            }
        }

        return t;
    }

    private String construirDetalle(Servicio s) {
        StringBuilder sb = new StringBuilder();
        if (s.getOrigen() != null && !s.getOrigen().isBlank()) {
            sb.append(s.getOrigen().toUpperCase());
        }
        if (s.getDestino() != null && !s.getDestino().isBlank()) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(s.getDestino().toUpperCase());
        }
        if (sb.length() == 0) sb.append("SERVICIO DE TRANSPORTE");
        return sb.toString();
    }

    private void agregarCeldaTabla(PdfPTable t, String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_NORMAL));
        c.setHorizontalAlignment(align);
        c.setPadding(5);
        c.setBorderWidth(0.5f);
        c.setBorderWidthTop(0);
        c.setBorderColor(Color.BLACK);
        t.addCell(c);
    }

    private void agregarCeldaDetalle(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setPadding(5);
        c.setBorderWidth(0.5f);
        c.setBorderWidthTop(0);
        c.setBorderColor(Color.BLACK);
        t.addCell(c);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. PIE: acuse de recibo (centro) + totales (derecha)
    // ═════════════════════════════════════════════════════════════════════════
    private PdfPTable pie(Factura f) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{35, 38, 27});

        // ── Columna izquierda: timbre SII ─────────────────────────────────
        PdfPCell cTimbre = new PdfPCell();
        cTimbre.setBorder(Rectangle.NO_BORDER);
        cTimbre.setPaddingTop(4);
        Paragraph pTimbre = new Paragraph();
        pTimbre.add(new Chunk("Timbre Electrónico S.I.I.\n", F_SMALL_B));
        pTimbre.add(new Chunk("Resolución 80 del 22/08/2014\n", F_SMALL));
        pTimbre.add(new Chunk("Verifique documento: www.sii.cl", F_SMALL));
        cTimbre.addElement(pTimbre);
        t.addCell(cTimbre);

        // ── Columna central: acuse de recibo ──────────────────────────────
        PdfPCell cAcuse = celdaAcuse();
        t.addCell(cAcuse);

        // ── Columna derecha: totales ───────────────────────────────────────
        PdfPCell cTotales = celdaTotales(f);
        t.addCell(cTotales);

        return t;
    }

    private PdfPCell celdaAcuse() {
        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try { inner.setWidths(new float[]{30, 70}); } catch (Exception ignored) {}

        // Título
        PdfPCell titulo = new PdfPCell(new Phrase("ACUSE DE RECIBO", F_SMALL_B));
        titulo.setColspan(2);
        titulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        titulo.setPadding(4);
        titulo.setBorderWidth(0.5f);
        titulo.setBorderColor(Color.BLACK);
        inner.addCell(titulo);

        String[] labels = {"NOMBRE", "R.U.T.", "FECHA", "RECINTO", "FIRMA"};
        for (String lbl : labels) {
            PdfPCell cL = new PdfPCell(new Phrase(lbl + " :", F_SMALL_B));
            cL.setPadding(4);
            cL.setBorderWidth(0.5f);
            cL.setBorderWidthTop(0);
            cL.setBorderColor(Color.BLACK);
            inner.addCell(cL);

            PdfPCell cV = new PdfPCell(new Phrase("_______________", F_SMALL));
            cV.setPadding(4);
            cV.setBorderWidth(0.5f);
            cV.setBorderWidthTop(0);
            cV.setBorderColor(Color.BLACK);
            inner.addCell(cV);
        }

        // Texto legal debajo
        PdfPCell cLegal = new PdfPCell();
        cLegal.setColspan(2);
        cLegal.setBorderWidth(0.5f);
        cLegal.setBorderWidthTop(0);
        cLegal.setBorderColor(Color.BLACK);
        cLegal.setPadding(4);
        cLegal.addElement(new Paragraph(
            "EL ACUSE DE RECIBO QUE SE DECLARA EN ESTE ACTO, " +
            "DE ACUERDO A LO DISPUESTO EN LA LETRA b) DEL ART.4 " +
            "Y LA LETRA c) DEL ART.5 DE LA LEY 19.983, ACREDITA " +
            "QUE LA ENTREGA DE MERCADERÍA(S) O SERVICIOS(S) " +
            "PRESTADO(S) HA(N) SIDO RECIBIDO(S) EN TOTAL CONFORMIDAD.",
            new Font(Font.HELVETICA, 6, Font.NORMAL, Color.BLACK)
        ));
        inner.addCell(cLegal);

        PdfPCell exterior = new PdfPCell(inner);
        exterior.setPadding(0);
        exterior.setBorderWidth(0.5f);
        exterior.setBorderColor(Color.BLACK);
        return exterior;
    }

    private PdfPCell celdaTotales(Factura f) {
        BigDecimal subtotal = f.getSubtotal() != null ? f.getSubtotal() : BigDecimal.ZERO;
        BigDecimal iva      = f.getIva()      != null ? f.getIva()      : BigDecimal.ZERO;
        BigDecimal total    = f.getTotal()    != null ? f.getTotal()    : BigDecimal.ZERO;

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try { inner.setWidths(new float[]{55, 45}); } catch (Exception ignored) {}

        agregarFilaTotales(inner, "DESCUENTO",  "$ 0");
        agregarFilaTotales(inner, "NETO",       "$ " + CLP.format(subtotal));
        agregarFilaTotales(inner, "EXENTO",     "$ 0");
        agregarFilaTotales(inner, "I.V.A. 19%", "$ " + CLP.format(iva));
        agregarFilaTotalesDestacada(inner, "TOTAL", "$ " + CLP.format(total));

        PdfPCell exterior = new PdfPCell(inner);
        exterior.setPadding(0);
        exterior.setBorderWidth(0.5f);
        exterior.setBorderColor(Color.BLACK);
        return exterior;
    }

    private void agregarFilaTotales(PdfPTable t, String label, String valor) {
        PdfPCell cL = new PdfPCell(new Phrase(label, F_TOTAL_LBL));
        cL.setPadding(5);
        cL.setBorderWidth(0.5f);
        cL.setBorderColor(Color.BLACK);
        t.addCell(cL);

        PdfPCell cV = new PdfPCell(new Phrase(valor, F_TOTAL_VAL));
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cV.setPadding(5);
        cV.setBorderWidth(0.5f);
        cV.setBorderColor(Color.BLACK);
        t.addCell(cV);
    }

    private void agregarFilaTotalesDestacada(PdfPTable t, String label, String valor) {
        PdfPCell cL = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)));
        cL.setPadding(6);
        cL.setBorderWidth(1.5f);
        cL.setBorderColor(Color.BLACK);
        t.addCell(cL);

        PdfPCell cV = new PdfPCell(new Phrase(valor, new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)));
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cV.setPadding(6);
        cV.setBorderWidth(1.5f);
        cV.setBorderColor(Color.BLACK);
        t.addCell(cV);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Utilidades
    // ═════════════════════════════════════════════════════════════════════════
    private Paragraph espacio(float height) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(height);
        return p;
    }

    private PdfPTable lineaHorizontal() throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColor(Color.BLACK);
        c.setPadding(0);
        t.addCell(c);
        return t;
    }

    private String safe(String s) {
        return (s != null && !s.isBlank()) ? s : "";
    }

    private String mesEnLetras(int mes) {
        String[] meses = {"", "enero", "febrero", "marzo", "abril", "mayo", "junio",
                          "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        return (mes >= 1 && mes <= 12) ? meses[mes] : "";
    }
}
