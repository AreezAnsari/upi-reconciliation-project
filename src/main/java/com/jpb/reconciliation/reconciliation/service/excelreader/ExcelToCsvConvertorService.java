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

    public void convertExcelToCsv(File excelFile) throws IOException {
        String csvFilePath = excelFile.getPath().replaceFirst("\\..*", ".csv");

        try (Workbook workbook = WorkbookFactory.create(excelFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean startProcessing = false;

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
                            writeRowToCsv(row, writer);
                        }
                    }
                    continue;
                }
                if (isRowEmpty(row) || isFooterRow(row)) {
                    continue;
                }

                writeRowToCsv(row, writer);
            }

        } catch (Exception e) {
            logger.error("Error converting Excel to CSV", e);
            throw new IOException("Failed to convert Excel file.", e);
        }
    }

    private void writeRowToCsv(Row row, PrintWriter writer) {
        StringBuilder line = new StringBuilder();
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String cellValue = getCellValueAsString(cell);
            line.append(cellValue);
            // This is the change: removed the double quotes
            if (cellIterator.hasNext()) {
                line.append(",");
            }
        }
        // This is the change: remove the last comma if it exists
        if (line.length() > 0 && line.charAt(line.length() - 1) == ',') {
            line.setLength(line.length() - 1);
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