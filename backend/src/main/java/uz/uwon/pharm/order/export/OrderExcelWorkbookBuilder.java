package uz.uwon.pharm.order.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TZ_excel_export_buyurtmalar.md maketiga mos .xlsx yaratadi.
 */
public final class OrderExcelWorkbookBuilder {

    private static final DateTimeFormatter DATE_CELL = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final byte[] BLUE = hex("B3CEFB");
    private static final byte[] GIFT_HDR = hex("F7B4AE");
    private static final byte[] GRAY = hex("D9D9D9");
    private static final byte[] GIFT_BODY = hex("FBD9D7");
    private OrderExcelWorkbookBuilder() {}

    public static byte[] build(
            String regionName,
            LocalDate date,
            List<OrderExportRow> rows,
            List<OrderExportProperties.ProductColumn> products,
            List<OrderExportProperties.ProductColumn> gifts
    ) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String sheetName = sanitizeSheetName(shortRegion(regionName));
            XSSFSheet sheet = wb.createSheet(sheetName);

            Styles styles = Styles.create(wb);

            writeTitleRow(sheet, styles, regionName, date);
            writeHeaderRow(sheet, styles, products, gifts);
            int dataStart = 2; // 0-based → Excel row 3
            int dataEnd = writeDataRows(sheet, styles, rows, products, gifts, dataStart);
            int totalRow = dataEnd + 2; // one blank row after data
            writeTotalRow(sheet, styles, products, gifts, dataStart, dataEnd, totalRow);

            applyColumnWidths(sheet);
            hideReserveColumns(sheet);
            freeze(sheet);
            applyDuplicateRules(sheet, dataStart, Math.max(dataEnd, dataStart));
            applyPrintSetup(wb, sheet, totalRow);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void writeTitleRow(XSSFSheet sheet, Styles styles, String regionName, LocalDate date) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(36f);

        for (int c = 0; c <= 29; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(styles.titleBlue);
        }

        row.getCell(1).setCellValue(date.format(DATE_CELL));
        row.getCell(1).setCellStyle(styles.titleBlue);

        row.getCell(2).setCellValue(shortRegion(regionName) + " viloyati");
        row.getCell(2).setCellStyle(styles.titleBlueBig);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 5, 15)); // F1:P1
        row.getCell(5).setCellValue("Mahsulotlar");
        row.getCell(5).setCellStyle(styles.titleBlueBig);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 16, 26)); // Q1:AA1
        for (int c = 16; c <= 26; c++) {
            row.getCell(c).setCellStyle(styles.titleGift);
        }
        row.getCell(16).setCellValue("Sovg'alar");
        row.getCell(16).setCellStyle(styles.titleGiftBig);
    }

    private static void writeHeaderRow(
            XSSFSheet sheet,
            Styles styles,
            List<OrderExportProperties.ProductColumn> products,
            List<OrderExportProperties.ProductColumn> gifts
    ) {
        Row row = sheet.createRow(1);
        row.setHeightInPoints(131f);

        String[] fixed = {
                "", "F.I.O", "Manzil", "Telofon raqam", "Buyurtma soni"
        };
        for (int i = 0; i < fixed.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(fixed[i]);
            cell.setCellStyle(i == 0 ? styles.headerBlue : styles.headerBlueVertical);
        }

        for (OrderExportProperties.ProductColumn p : products) {
            int col = colIndex(p.getColumn());
            Cell cell = row.createCell(col);
            cell.setCellValue(p.getTitle());
            cell.setCellStyle(styles.headerBlueVertical);
        }
        // O, P reserve
        for (int c : new int[]{14, 15}) {
            Cell cell = row.createCell(c);
            cell.setCellValue("Пустой");
            cell.setCellStyle(styles.headerBlueVertical);
        }

        for (OrderExportProperties.ProductColumn g : gifts) {
            int col = colIndex(g.getColumn());
            Cell cell = row.createCell(col);
            cell.setCellValue(g.getTitle());
            cell.setCellStyle(styles.headerGiftVertical);
        }
        for (int c : new int[]{24, 25, 26}) {
            Cell cell = row.createCell(c);
            cell.setCellValue("Пустой");
            cell.setCellStyle(styles.headerGiftVertical);
        }

        Cell note = row.createCell(27);
        note.setCellValue("Изоҳ");
        note.setCellStyle(styles.headerBlueVertical);
        Cell spec = row.createCell(28);
        spec.setCellValue("Мутахассис");
        spec.setCellStyle(styles.headerBlueVertical);
        Cell order = row.createCell(29);
        order.setCellValue("Заказ");
        order.setCellStyle(styles.headerBlueVertical);
    }

    private static int writeDataRows(
            XSSFSheet sheet,
            Styles styles,
            List<OrderExportRow> rows,
            List<OrderExportProperties.ProductColumn> products,
            List<OrderExportProperties.ProductColumn> gifts,
            int startRow
    ) {
        if (rows.isEmpty()) {
            // still create one empty formula row so totals work
            writeOneDataRow(sheet, styles, OrderExportRow.empty(), products, gifts, startRow, true);
            return startRow;
        }
        int r = startRow;
        for (OrderExportRow row : rows) {
            writeOneDataRow(sheet, styles, row, products, gifts, r, false);
            r++;
        }
        return r - 1;
    }

    private static void writeOneDataRow(
            XSSFSheet sheet,
            Styles styles,
            OrderExportRow data,
            List<OrderExportProperties.ProductColumn> products,
            List<OrderExportProperties.ProductColumn> gifts,
            int r,
            boolean blank
    ) {
        Row row = sheet.createRow(r);
        int excelRow = r + 1;

        Cell a = row.createCell(0);
        a.setCellFormula("IF(B" + excelRow + "<>\"\",COUNTA($B$3:B" + excelRow + "),\"\")");
        a.setCellStyle(styles.bodyCenter);

        Cell b = row.createCell(1);
        b.setCellValue(blank ? "" : nullToEmpty(data.customerName()));
        b.setCellStyle(styles.bodyGrayLeft);

        Cell c = row.createCell(2);
        c.setCellValue(blank ? "" : nullToEmpty(data.address()));
        c.setCellStyle(styles.bodyGrayLeft);

        Cell d = row.createCell(3);
        if (!blank && data.phoneDigits() != null && !data.phoneDigits().isBlank()) {
            try {
                d.setCellValue(Long.parseLong(data.phoneDigits()));
            } catch (NumberFormatException e) {
                d.setCellValue(data.phoneDigits());
            }
        }
        d.setCellStyle(styles.bodyGrayPhone);

        Cell e = row.createCell(4);
        if (!blank) {
            e.setCellValue(data.orderCount());
        }
        e.setCellStyle(styles.bodyGrayCenter);

        for (OrderExportProperties.ProductColumn p : products) {
            int col = colIndex(p.getColumn());
            Cell cell = row.createCell(col);
            BigDecimal qty = blank ? null : data.items().get(p.getColumn());
            if (qty != null && qty.compareTo(BigDecimal.ZERO) != 0) {
                cell.setCellValue(qty.doubleValue());
            }
            cell.setCellStyle(styles.bodyCenter);
        }
        for (int col : new int[]{14, 15}) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(styles.bodyCenter);
        }

        for (OrderExportProperties.ProductColumn g : gifts) {
            int col = colIndex(g.getColumn());
            Cell cell = row.createCell(col);
            BigDecimal qty = blank ? null : data.gifts().get(g.getColumn());
            if (qty != null && qty.compareTo(BigDecimal.ZERO) != 0) {
                cell.setCellValue(qty.doubleValue());
            }
            cell.setCellStyle(styles.bodyGiftCenter);
        }
        for (int col : new int[]{24, 25, 26}) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(styles.bodyGiftCenter);
        }

        Cell note = row.createCell(27);
        note.setCellValue(blank ? "" : nullToEmpty(data.note()));
        note.setCellStyle(styles.bodyCenter);

        Cell spec = row.createCell(28);
        spec.setCellValue(blank ? "" : nullToEmpty(data.specialist()));
        spec.setCellStyle(styles.bodyCenter);

        Cell ad = row.createCell(29);
        ad.setCellFormula(buildOrderFormula(products, excelRow));
        ad.setCellStyle(styles.bodyOrderSum);
    }

    private static String buildOrderFormula(List<OrderExportProperties.ProductColumn> products, int excelRow) {
        StringBuilder sb = new StringBuilder();
        for (OrderExportProperties.ProductColumn p : products) {
            if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) == 0) continue;
            if (!sb.isEmpty()) sb.append('+');
            sb.append(p.getColumn()).append(excelRow).append('*').append(p.getPrice().toPlainString());
        }
        return sb.isEmpty() ? "0" : sb.toString();
    }

    private static void writeTotalRow(
            XSSFSheet sheet,
            Styles styles,
            List<OrderExportProperties.ProductColumn> products,
            List<OrderExportProperties.ProductColumn> gifts,
            int dataStart,
            int dataEnd,
            int totalRow
    ) {
        Row row = sheet.createRow(totalRow);
        int first = dataStart + 1;
        int last = dataEnd + 1;

        for (int c = 0; c <= 29; c++) {
            row.createCell(c).setCellStyle(styles.bodyCenter);
        }

        // E .. AA SUBTOTAL
        for (int c = 4; c <= 26; c++) {
            String col = CellReference.convertNumToColString(c);
            Cell cell = row.getCell(c);
            cell.setCellFormula("SUBTOTAL(9," + col + first + ":" + col + last + ")");
            boolean gift = c >= 16;
            cell.setCellStyle(gift ? styles.totalGift : styles.totalBlue);
        }

        Cell label = row.getCell(27);
        label.setCellValue("ИТОГ:");
        label.setCellStyle(styles.totalBlueBold);

        sheet.addMergedRegion(new CellRangeAddress(totalRow, totalRow, 28, 29));
        Cell sum = row.getCell(28);
        sum.setCellFormula("SUBTOTAL(9,AD" + first + ":AD" + last + ")");
        sum.setCellStyle(styles.totalSum);
        row.getCell(29).setCellStyle(styles.totalSum);
    }

    private static void applyColumnWidths(XSSFSheet sheet) {
        sheet.setColumnWidth(0, chars(5.7));
        sheet.setColumnWidth(1, chars(15.7));
        sheet.setColumnWidth(2, chars(30.7));
        sheet.setColumnWidth(3, chars(20.7));
        for (int c = 4; c <= 13; c++) sheet.setColumnWidth(c, chars(5.7));
        for (int c = 16; c <= 23; c++) sheet.setColumnWidth(c, chars(5.7));
        sheet.setColumnWidth(27, chars(15.7));
        sheet.setColumnWidth(28, chars(20.7));
        sheet.setColumnWidth(29, chars(20.7));
    }

    private static void hideReserveColumns(XSSFSheet sheet) {
        for (int c : new int[]{14, 15, 24, 25, 26}) {
            sheet.setColumnHidden(c, true);
            sheet.setColumnWidth(c, chars(5.7));
        }
    }

    private static void freeze(XSSFSheet sheet) {
        sheet.createFreezePane(0, 2); // freeze rows 1-2 → A3
    }

    private static void applyDuplicateRules(XSSFSheet sheet, int dataStart, int dataEnd) {
        if (dataEnd < dataStart) return;
        try {
            SheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
            int first = dataStart + 1;
            int last = dataEnd + 1;

            ConditionalFormattingRule dupC = cf.createConditionalFormattingRule(
                    "AND(C" + first + "<>\"\",COUNTIF($C$" + first + ":$C$" + last + ",C" + first + ")>1)");
            PatternFormatting fillC = dupC.createPatternFormatting();
            fillC.setFillBackgroundColor(IndexedColors.ROSE.getIndex());
            FontFormatting fontC = dupC.createFontFormatting();
            fontC.setFontColorIndex(IndexedColors.DARK_RED.getIndex());

            ConditionalFormattingRule dupD = cf.createConditionalFormattingRule(
                    "AND(D" + first + "<>\"\",COUNTIF($D$" + first + ":$D$" + last + ",D" + first + ")>1)");
            PatternFormatting fillD = dupD.createPatternFormatting();
            fillD.setFillBackgroundColor(IndexedColors.ROSE.getIndex());
            FontFormatting fontD = dupD.createFontFormatting();
            fontD.setFontColorIndex(IndexedColors.DARK_RED.getIndex());

            cf.addConditionalFormatting(new CellRangeAddress[]{
                    new CellRangeAddress(dataStart, dataEnd, 2, 2)
            }, dupC);
            cf.addConditionalFormatting(new CellRangeAddress[]{
                    new CellRangeAddress(dataStart, dataEnd, 3, 3)
            }, dupD);
        } catch (Throwable ignored) {
            // Conditional formatting ixtiyoriy — asosiy eksport ishlashi kerak
        }
    }

    private static void applyPrintSetup(XSSFWorkbook wb, XSSFSheet sheet, int lastRow) {
        sheet.setAutobreaks(true);
        PrintSetup ps = sheet.getPrintSetup();
        ps.setLandscape(true);
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        sheet.setFitToPage(true);
        wb.setPrintArea(wb.getSheetIndex(sheet), 0, 29, 0, lastRow);
        sheet.setMargin(Sheet.LeftMargin, 0.2);
        sheet.setMargin(Sheet.RightMargin, 0.2);
        sheet.setMargin(Sheet.TopMargin, 0.4);
        sheet.setMargin(Sheet.BottomMargin, 0.4);
    }

    private static int colIndex(String letter) {
        return CellReference.convertColStringToIndex(letter);
    }

    private static int chars(double w) {
        return (int) (w * 256);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String shortRegion(String name) {
        if (name == null || name.isBlank()) return "Region";
        return name.replace(" viloyati", "").replace(" Вилояти", "").trim();
    }

    private static String sanitizeSheetName(String name) {
        String n = name.replaceAll("[\\\\/?*\\[\\]]", " ").trim();
        if (n.length() > 31) n = n.substring(0, 31);
        return n.isEmpty() ? "Sheet1" : n;
    }

    private static byte[] hex(String rgb) {
        return new byte[]{
                (byte) Integer.parseInt(rgb.substring(0, 2), 16),
                (byte) Integer.parseInt(rgb.substring(2, 4), 16),
                (byte) Integer.parseInt(rgb.substring(4, 6), 16)
        };
    }

    private record Styles(
            XSSFCellStyle titleBlue,
            XSSFCellStyle titleBlueBig,
            XSSFCellStyle titleGift,
            XSSFCellStyle titleGiftBig,
            XSSFCellStyle headerBlue,
            XSSFCellStyle headerBlueVertical,
            XSSFCellStyle headerGiftVertical,
            XSSFCellStyle bodyCenter,
            XSSFCellStyle bodyGrayLeft,
            XSSFCellStyle bodyGrayCenter,
            XSSFCellStyle bodyGrayPhone,
            XSSFCellStyle bodyGiftCenter,
            XSSFCellStyle bodyOrderSum,
            XSSFCellStyle totalBlue,
            XSSFCellStyle totalGift,
            XSSFCellStyle totalBlueBold,
            XSSFCellStyle totalSum
    ) {
        static Styles create(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();

            XSSFFont calibri12 = font(wb, "Calibri", 12, false);
            XSSFFont calibri14 = font(wb, "Calibri", 14, false);
            XSSFFont calibri14b = font(wb, "Calibri", 14, true);
            XSSFFont arial16b = font(wb, "Arial", 16, true);

            XSSFCellStyle titleBlue = base(wb, BLUE, calibri14, HorizontalAlignment.CENTER);
            XSSFCellStyle titleBlueBig = base(wb, BLUE, arial16b, HorizontalAlignment.CENTER);
            XSSFCellStyle titleGift = base(wb, GIFT_HDR, calibri14, HorizontalAlignment.CENTER);
            XSSFCellStyle titleGiftBig = base(wb, GIFT_HDR, arial16b, HorizontalAlignment.CENTER);

            XSSFCellStyle headerBlue = base(wb, BLUE, calibri14, HorizontalAlignment.CENTER);
            XSSFCellStyle headerBlueVertical = base(wb, BLUE, calibri14, HorizontalAlignment.CENTER);
            headerBlueVertical.setRotation((short) 90);
            XSSFCellStyle headerGiftVertical = base(wb, GIFT_HDR, calibri14, HorizontalAlignment.CENTER);
            headerGiftVertical.setRotation((short) 90);

            XSSFCellStyle bodyCenter = base(wb, null, calibri12, HorizontalAlignment.CENTER);
            XSSFCellStyle bodyGrayLeft = base(wb, GRAY, calibri12, HorizontalAlignment.LEFT);
            XSSFCellStyle bodyGrayCenter = base(wb, GRAY, calibri12, HorizontalAlignment.CENTER);
            XSSFCellStyle bodyGrayPhone = base(wb, GRAY, calibri12, HorizontalAlignment.CENTER);
            bodyGrayPhone.setDataFormat(df.getFormat("0"));
            XSSFCellStyle bodyGiftCenter = base(wb, GIFT_BODY, calibri12, HorizontalAlignment.CENTER);
            XSSFCellStyle bodyOrderSum = base(wb, null, calibri14b, HorizontalAlignment.CENTER);
            bodyOrderSum.setDataFormat(df.getFormat("#,##0"));

            XSSFCellStyle totalBlue = base(wb, BLUE, calibri12, HorizontalAlignment.CENTER);
            XSSFCellStyle totalGift = base(wb, GIFT_HDR, calibri12, HorizontalAlignment.CENTER);
            XSSFCellStyle totalBlueBold = base(wb, BLUE, calibri14b, HorizontalAlignment.CENTER);
            XSSFCellStyle totalSum = base(wb, BLUE, calibri14b, HorizontalAlignment.CENTER);
            totalSum.setDataFormat(df.getFormat("#,##0"));

            return new Styles(
                    titleBlue, titleBlueBig, titleGift, titleGiftBig,
                    headerBlue, headerBlueVertical, headerGiftVertical,
                    bodyCenter, bodyGrayLeft, bodyGrayCenter, bodyGrayPhone, bodyGiftCenter, bodyOrderSum,
                    totalBlue, totalGift, totalBlueBold, totalSum
            );
        }

        private static XSSFFont font(XSSFWorkbook wb, String name, int size, boolean bold) {
            XSSFFont f = wb.createFont();
            f.setFontName(name);
            f.setFontHeightInPoints((short) size);
            f.setBold(bold);
            return f;
        }

        private static XSSFCellStyle base(XSSFWorkbook wb, byte[] rgb, XSSFFont font, HorizontalAlignment align) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFont(font);
            s.setAlignment(align);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(true);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            if (rgb != null) {
                s.setFillForegroundColor(new XSSFColor(rgb, null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return s;
        }
    }
}
