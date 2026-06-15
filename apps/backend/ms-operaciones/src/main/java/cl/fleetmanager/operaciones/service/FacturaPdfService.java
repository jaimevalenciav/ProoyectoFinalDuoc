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

    // ── Colores corporativos ──────────────────────────────────────────────────
    private static final Color AZUL_OSCURO  = new Color(0x1B, 0x2C, 0x40);  // #1B2C40
    private static final Color AZUL_MED    = new Color(0x24, 0x5E, 0x8E);  // #245E8E
    private static final Color AZUL_CLARO  = new Color(0xDF, 0xEB, 0xF7);  // #DFEBF7
    private static final Color GRIS_BORDE  = new Color(0xE2, 0xE8, 0xF0);  // #E2E8F0
    private static final Color GRIS_FONDO  = new Color(0xF8, 0xFA, 0xFC);  // #F8FAFC
    private static final Color VERDE       = new Color(0x16, 0xA3, 0x4A);  // #16A34A
    private static final Color ROJO        = new Color(0xDC, 0x26, 0x26);  // #DC2626

    // ── Formatos ─────────────────────────────────────────────────────────────
    private static final NumberFormat CLP  = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(String facturaId) throws Exception {
        Factura f = facturaRepo.findById(facturaId)
            .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + facturaId));

        Cliente c = clienteRepo.findById(f.getClienteId()).orElse(null);
        List<Servicio> servicios = servicioRepo.findByFacturaId(facturaId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        doc.addTitle("Factura " + f.getNumFactura());
        doc.addAuthor("TruckManager Pro");
        doc.open();

        // ── 1. Cabecera ─────────────────────────────────────────────────────
        agregarCabecera(doc, writer, f);

        doc.add(Chunk.NEWLINE);

        // ── 2. Datos del emisor y receptor ─────────────────────────────────
        agregarDatosPartes(doc, c);

        doc.add(Chunk.NEWLINE);

        // ── 3. Tabla de servicios ───────────────────────────────────────────
        if (!servicios.isEmpty()) {
            agregarTablaServicios(doc, servicios);
            doc.add(Chunk.NEWLINE);
        }

        // ── 4. Totales ──────────────────────────────────────────────────────
        agregarTotales(doc, f);

        // ── 5. Notas ────────────────────────────────────────────────────────
        if (f.getNotas() != null && !f.getNotas().isBlank()) {
            doc.add(Chunk.NEWLINE);
            agregarNotas(doc, f.getNotas());
        }

        // ── 6. Pie ──────────────────────────────────────────────────────────
        agregarPie(doc);

        doc.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarCabecera(Document doc, PdfWriter writer, Factura f) throws Exception {
        // Banda superior azul oscuro
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorFill(AZUL_OSCURO);
        float y = doc.top() + 10;
        cb.rectangle(doc.left() - 50, y - 70, doc.right() - doc.left() + 100, 70);
        cb.fill();

        // Logo / nombre empresa (izquierda)
        Font fEmpresa = new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE);
        Font fSlogan  = new Font(Font.HELVETICA,  9, Font.NORMAL, new Color(0xBF, 0xDB, 0xF4));

        PdfPTable tCabecera = new PdfPTable(2);
        tCabecera.setWidthPercentage(100);
        tCabecera.setWidths(new float[]{60, 40});
        tCabecera.setSpacingBefore(0);

        // Celda izquierda — nombre
        PdfPCell cEmpresa = new PdfPCell();
        cEmpresa.setBorder(Rectangle.NO_BORDER);
        cEmpresa.setBackgroundColor(AZUL_OSCURO);
        cEmpresa.setPaddingTop(10);
        cEmpresa.setPaddingBottom(10);
        cEmpresa.addElement(new Paragraph("TruckManager Pro", fEmpresa));
        cEmpresa.addElement(new Paragraph("Gestión Inteligente de Flota", fSlogan));
        tCabecera.addCell(cEmpresa);

        // Celda derecha — FACTURA + número
        PdfPCell cFactura = new PdfPCell();
        cFactura.setBorder(Rectangle.NO_BORDER);
        cFactura.setBackgroundColor(AZUL_OSCURO);
        cFactura.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cFactura.setPaddingTop(8);
        cFactura.setPaddingBottom(8);
        cFactura.setPaddingRight(2);

        Font fTitulo  = new Font(Font.HELVETICA, 24, Font.BOLD, Color.WHITE);
        Font fNumero  = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0x93, 0xC5, 0xFD));
        Font fFecha   = new Font(Font.HELVETICA,  9, Font.NORMAL, new Color(0xBF, 0xDB, 0xF4));

        Paragraph pTitulo = new Paragraph("FACTURA", fTitulo);
        pTitulo.setAlignment(Element.ALIGN_RIGHT);
        cFactura.addElement(pTitulo);

        Paragraph pNum = new Paragraph(f.getNumFactura(), fNumero);
        pNum.setAlignment(Element.ALIGN_RIGHT);
        cFactura.addElement(pNum);

        String fecha = f.getFechaEmision() != null ? f.getFechaEmision().format(FMT_FECHA) : "—";
        Paragraph pFecha = new Paragraph("Fecha de emisión: " + fecha, fFecha);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        cFactura.addElement(pFecha);

        // Estado
        String estadoLabel = f.getEstado() != null ? f.getEstado() : "EMITIDA";
        Color colorEstado = "ANULADA".equals(estadoLabel) ? ROJO
                          : "PAGADA".equals(estadoLabel)  ? VERDE
                          : new Color(0x3B, 0x82, 0xF6);
        Font fEstado = new Font(Font.HELVETICA, 8, Font.BOLD, colorEstado);
        Paragraph pEstado = new Paragraph("● " + estadoLabel, fEstado);
        pEstado.setAlignment(Element.ALIGN_RIGHT);
        cFactura.addElement(pEstado);

        tCabecera.addCell(cFactura);
        doc.add(tCabecera);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarDatosPartes(Document doc, Cliente c) throws Exception {
        Font fLabel  = new Font(Font.HELVETICA, 8,  Font.BOLD,   AZUL_MED);
        Font fValor  = new Font(Font.HELVETICA, 10, Font.BOLD,   AZUL_OSCURO);
        Font fValor2 = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{50, 50});
        tabla.setSpacingBefore(4);

        // Celda emisor
        PdfPCell cEmisor = crearCeldaDatos(
            "DATOS DEL EMISOR", fLabel,
            new String[]{"TruckManager Pro S.A.", "76.123.456-7", "Servicios de Transporte y Logística",
                         "Santiago, Región Metropolitana"},
            new Font[]{fValor, fValor2, fValor2, fValor2}
        );
        tabla.addCell(cEmisor);

        // Celda receptor
        if (c != null) {
            PdfPCell cReceptor = crearCeldaDatos(
                "RECEPTOR (CLIENTE)", fLabel,
                new String[]{
                    c.getRazonSocial() != null ? c.getRazonSocial() : "—",
                    "RUT: " + (c.getRut() != null ? c.getRut() : "—"),
                    c.getGiro()    != null ? c.getGiro()    : "",
                    c.getCiudad()  != null ? c.getCiudad()  : "",
                    c.getEmail()   != null ? c.getEmail()   : "",
                    c.getTelefono() != null ? c.getTelefono() : "",
                },
                new Font[]{fValor, fValor2, fValor2, fValor2, fValor2, fValor2}
            );
            tabla.addCell(cReceptor);
        } else {
            tabla.addCell(celdaVacia());
        }

        doc.add(tabla);
    }

    private PdfPCell crearCeldaDatos(String titulo, Font fLabel, String[] lineas, Font[] fuentes) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(GRIS_BORDE);
        celda.setPadding(12);
        celda.setBackgroundColor(GRIS_FONDO);

        Paragraph pTitulo = new Paragraph(titulo, fLabel);
        pTitulo.setSpacingAfter(4);
        celda.addElement(pTitulo);

        for (int i = 0; i < lineas.length; i++) {
            if (lineas[i] != null && !lineas[i].isBlank()) {
                Font f = (i < fuentes.length) ? fuentes[i] : fuentes[fuentes.length - 1];
                celda.addElement(new Paragraph(lineas[i], f));
            }
        }
        return celda;
    }

    private PdfPCell celdaVacia() {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarTablaServicios(Document doc, List<Servicio> servicios) throws Exception {
        Font fHead  = new Font(Font.HELVETICA, 9, Font.BOLD,   Color.WHITE);
        Font fBody  = new Font(Font.HELVETICA, 9, Font.NORMAL, AZUL_OSCURO);
        Font fMonto = new Font(Font.HELVETICA, 9, Font.BOLD,   AZUL_OSCURO);

        // Título sección
        Font fSeccion = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL_MED);
        Paragraph pSec = new Paragraph("DETALLE DE SERVICIOS", fSeccion);
        pSec.setSpacingAfter(4);
        doc.add(pSec);

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{12, 28, 28, 16, 16});

        // Encabezados
        String[] headers = {"N° Serv.", "Origen", "Destino", "Neto (CLP)", "Total (CLP)"};
        for (String h : headers) {
            PdfPCell th = new PdfPCell(new Phrase(h, fHead));
            th.setBackgroundColor(AZUL_MED);
            th.setPadding(7);
            th.setBorder(Rectangle.NO_BORDER);
            th.setHorizontalAlignment(h.endsWith("(CLP)") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            tabla.addCell(th);
        }

        // Filas
        boolean alterno = false;
        for (Servicio s : servicios) {
            Color fondo = alterno ? AZUL_CLARO : Color.WHITE;
            alterno = !alterno;

            tabla.addCell(celdaTabla(safe(s.getNumServicio()), fBody, Element.ALIGN_LEFT,  fondo));
            tabla.addCell(celdaTabla(safe(s.getOrigen()),      fBody, Element.ALIGN_LEFT,  fondo));
            tabla.addCell(celdaTabla(safe(s.getDestino()),     fBody, Element.ALIGN_LEFT,  fondo));
            tabla.addCell(celdaTabla(formatoClp(s.getValorNeto()),  fMonto, Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celdaTabla(formatoClp(s.getValorTotal()), fMonto, Element.ALIGN_RIGHT, fondo));
        }

        doc.add(tabla);
    }

    private PdfPCell celdaTabla(String texto, Font fuente, int align, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setPadding(6);
        c.setBorderColor(GRIS_BORDE);
        c.setBorderWidth(0.5f);
        c.setHorizontalAlignment(align);
        c.setBackgroundColor(fondo);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarTotales(Document doc, Factura f) throws Exception {
        Font fLabel  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font fMonto  = new Font(Font.HELVETICA, 10, Font.BOLD,   AZUL_OSCURO);
        Font fTotal  = new Font(Font.HELVETICA, 13, Font.BOLD,   Color.WHITE);
        Font fTLbl   = new Font(Font.HELVETICA, 11, Font.BOLD,   Color.WHITE);

        // Tabla de totales alineada a la derecha (40% ancho)
        PdfPTable wrap = new PdfPTable(2);
        wrap.setWidthPercentage(100);
        wrap.setWidths(new float[]{60, 40});

        // Celda vacía izquierda
        PdfPCell vacio = new PdfPCell(new Phrase(" "));
        vacio.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(vacio);

        // Celda totales
        PdfPTable tTotales = new PdfPTable(2);
        tTotales.setWidthPercentage(100);

        agregarFilaTotales(tTotales, "Subtotal neto",  formatoClp(f.getSubtotal()),  fLabel, fMonto, GRIS_FONDO);
        agregarFilaTotales(tTotales, "IVA (19%)",      formatoClp(f.getIva()),       fLabel, fMonto, Color.WHITE);
        agregarFilaTotalesDestacada(tTotales, "TOTAL",  formatoClp(f.getTotal()), fTLbl, fTotal);

        PdfPCell cTotales = new PdfPCell(tTotales);
        cTotales.setBorder(Rectangle.BOX);
        cTotales.setBorderColor(GRIS_BORDE);
        cTotales.setPadding(0);
        wrap.addCell(cTotales);

        doc.add(wrap);
    }

    private void agregarFilaTotales(PdfPTable t, String lbl, String val,
                                    Font fLbl, Font fVal, Color fondo) {
        PdfPCell cL = new PdfPCell(new Phrase(lbl, fLbl));
        cL.setPadding(8); cL.setBorderColor(GRIS_BORDE); cL.setBackgroundColor(fondo);
        PdfPCell cV = new PdfPCell(new Phrase(val, fVal));
        cV.setPadding(8); cV.setBorderColor(GRIS_BORDE); cV.setBackgroundColor(fondo);
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cL); t.addCell(cV);
    }

    private void agregarFilaTotalesDestacada(PdfPTable t, String lbl, String val,
                                             Font fLbl, Font fVal) {
        PdfPCell cL = new PdfPCell(new Phrase(lbl, fLbl));
        cL.setPadding(10); cL.setBorder(Rectangle.NO_BORDER); cL.setBackgroundColor(AZUL_OSCURO);
        PdfPCell cV = new PdfPCell(new Phrase(val, fVal));
        cV.setPadding(10); cV.setBorder(Rectangle.NO_BORDER); cV.setBackgroundColor(AZUL_OSCURO);
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cL); t.addCell(cV);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarNotas(Document doc, String notas) throws Exception {
        Font fLbl  = new Font(Font.HELVETICA, 8, Font.BOLD,   AZUL_MED);
        Font fNota = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.LEFT);
        c.setBorderColorLeft(AZUL_MED);
        c.setBorderWidthLeft(3f);
        c.setBackgroundColor(AZUL_CLARO);
        c.setPadding(10);
        c.addElement(new Paragraph("OBSERVACIONES", fLbl));
        c.addElement(new Paragraph(notas, fNota));
        t.addCell(c);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void agregarPie(Document doc) throws Exception {
        Font fPie = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(0x94, 0xA3, 0xB8));
        Paragraph p = new Paragraph(
            "Documento generado electrónicamente por TruckManager Pro · " +
            "Este documento es válido como comprobante de prestación de servicios.",
            fPie
        );
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(20);
        doc.add(p);

        // Línea separadora
        Paragraph linea = new Paragraph("─".repeat(90), fPie);
        linea.setAlignment(Element.ALIGN_CENTER);
        doc.add(linea);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private String formatoClp(BigDecimal valor) {
        if (valor == null) return "$ 0";
        return CLP.format(valor.longValue());
    }

    private String safe(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }
}
