package com.jpb.reconciliation.reconciliation.service.excelreader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExcelToCsvConvertorService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelToCsvConvertorService.class);

    /**
     * V1 compatibility overload — untouched original hardcoded-header-name
     * behaviour, unchanged, so ExtractionController.java (V1) keeps working
     * exactly as before. V1 must never call the new headerLineCount-based
     * overload below; that one is V2-only (see ExtractionAiController.java).
     */
    public void convertExcelToCsv(File excelFile) throws IOException {
        String csvFilePath = excelFile.getPath().replaceFirst("\\..*", ".csv");

        try (Workbook workbook = WorkbookFactory.create(excelFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean startProcessing = false;
            int columnCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Find the actual header row by matching known first-column names.
                // Pehle metadata rows (report title, date range, bank name etc.) skip hoti hain.
                // Jab pehla cell ek known column header name se match kare tab hi processing start hoti hai.
                if (!startProcessing) {
                    Cell firstCell = row.getCell(0);
                    if (firstCell != null) {
                        String cellVal = getCellValueAsString(firstCell).trim();
                        // PEHLE: condition commented out thi → pehli non-empty row header ban jaati thi
                        //        (metadata row 1 header, rows 2-11 metadata data ban jaata tha → SQL*Loader fail)
                        // AB:    sirf matching column-name wali row header manate hain → metadata rows skip
                        if (cellVal.equalsIgnoreCase("S.NO")
                                || cellVal.equalsIgnoreCase("SNO")
                                || cellVal.equalsIgnoreCase("SR_NO")
                                || cellVal.equalsIgnoreCase("Description")) {
                            startProcessing = true;
                            columnCount = row.getLastCellNum();
                            writeRowToCsv(row, columnCount, writer);
                        }
                    }
                    continue;
                }
                if (isRowEmpty(row) || isFooterRow(row)) {
                    continue;
                }

                writeRowToCsv(row, columnCount, writer);
            }

        } catch (Exception e) {
            logger.error("Error converting Excel to CSV", e);
            throw new IOException("Failed to convert Excel file.", e);
        }
    }

    /**
     * V2-only overload — see FILE 4 in Code_Changes_LineByLine.txt for why
     * this exists separately from the V1 method above.
     */
    public void convertExcelToCsv(File excelFile, Integer headerLineCount) throws IOException {
        String csvFilePath = excelFile.getPath().replaceFirst("\\..*", ".csv");

        // Number of leading rows in the RAW excel file to skip before the real header
        // row is reached (report title / bank name / search-criteria rows etc. that
        // some source systems put above the actual column header — e.g. NEFT-ISO
        // reports have 11 such rows before the header). Template-configured, not
        // guessed from the header text, so it works for any file's header naming.
        int rowsToSkip = (headerLineCount != null && headerLineCount > 0) ? headerLineCount - 1 : 0;

        try (Workbook workbook = WorkbookFactory.create(excelFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            int rowIndex = 0;
            boolean headerWritten = false;
            int columnCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (rowIndex < rowsToSkip) {
                    rowIndex++;
                    continue;
                }
                if (!headerWritten) {
                    // This is the real header row (row after the configured skip count).
                    // Written as-is to the CSV — the downstream SQL*Loader control file
                    // always uses skip=1 to skip exactly this one header line.
                    columnCount = row.getLastCellNum();
                    writeRowToCsv(row, columnCount, writer);
                    headerWritten = true;
                    rowIndex++;
                    continue;
                }
                if (isRowEmpty(row) || isFooterRow(row)) {
                    rowIndex++;
                    continue;
                }

                writeRowToCsv(row, columnCount, writer);
                rowIndex++;
            }

        } catch (Exception e) {
            logger.error("Error converting Excel to CSV", e);
            throw new IOException("Failed to convert Excel file.", e);
        }
    }

    /**
     * Writes exactly columnCount comma-separated fields, one per column index.
     * Row.cellIterator() (the old approach) only visits cells POI actually
     * instantiated — a blank cell in the middle of a row (common for sparse
     * data written by tools like openpyxl) can be entirely absent from the
     * row's cell list, silently dropping that column and shifting every
     * later field left by one. Iterating by fixed index and reading each
     * cell with getCell() (returns null for missing/blank, rendered as "")
     * guarantees column alignment regardless of which cells exist.
     */
    private void writeRowToCsv(Row row, int columnCount, PrintWriter writer) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i);
            line.append(getCellValueAsString(cell));
            if (i < columnCount - 1) {
                line.append(",");
            }
        }
        writer.println(line.toString());
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && cell.toString().trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isFooterRow(Row row) {
        if (row == null) {
            return false;
        }
        Cell firstCell = row.getCell(0);
        if (firstCell != null && firstCell.getCellType() == CellType.STRING) {
       	 return firstCell.getStringCellValue().trim().startsWith("Note:") ||
                    firstCell.getStringCellValue().trim().startsWith("1.") ||
                    firstCell.getStringCellValue().trim().startsWith("2.") ||
                    firstCell.getStringCellValue().trim().startsWith("3.");
       }
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                // PEHLE: embedded newlines (\n) as-is return hote the
                //        → header cells jaise "Settlement Date\n(YYYY/MM/DD)" CSV mein 2 rows ban jaati thi
                //        → data cells jaise "Mr. Panna\nTiwari" bhi split hoti thi
                //        → SQL*Loader misaligned rows → ORA-01722 on SEQUENCE_NO / SNO
                // AB:    newlines replace karke single-line string return karo
                return cell.getStringCellValue()
                        .replace("\r\n", " ")
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .trim();
            case NUMERIC:
                // PEHLE: date cells bhi numeric treat hoti thi → serial number (46376) return hota tha
                //        SQL*Loader DATE "YYYY/MM/DD" parse nahi kar pata tha → row reject
                // AB:    date-formatted cells ko "yyyy/MM/dd" string mein convert karo
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy/MM/dd").format(cell.getDateCellValue());
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return String.valueOf((long) value);
                } else {
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}