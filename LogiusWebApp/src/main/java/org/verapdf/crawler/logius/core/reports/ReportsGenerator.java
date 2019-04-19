package org.verapdf.crawler.logius.core.reports;

import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.validation.error.ValidationError;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
@Service
public class ReportsGenerator {

    @Value("${logius.reports.notificationEmails}")
    private String notificationEmails;
    @Value("${logius.reports.odsTempFolder}")
    private String odsTempFolder;
    @Value("${logius.reports.odsTemplatePath}")
    private String odsTemplatePath;
    private ReportsGenerator() {

    }

    private static void fillSimpleSheet(List<String> documentsList,
                                        int sheetNumber, SpreadSheet spreadSheet) {
        Sheet sheet = spreadSheet.getSheet(sheetNumber);
        sheet.ensureColumnCount(1);
        sheet.ensureRowCount(documentsList.size());
        int i = 0;
        for (String document : documentsList) {
            sheet.setValueAt(document, 0, i++);
        }
    }

    private static void fillNonPDFA12Documents(List<DomainDocument> documentsList, SpreadSheet spreadSheet) {
        Sheet sheet = spreadSheet.getSheet(1);
        sheet.ensureColumnCount(5);
        sheet.ensureRowCount(getNonPDFA12DocumentsRowCount(documentsList) + 1);
        int i = 1;
        for (DomainDocument document : documentsList) {
            sheet.setValueAt(document.getDocumentUrl(), 0, i);
            Map<String, String> properties = document.getProperties();
            if (properties != null) {
                setProperty("flavour", 1, i, properties, sheet);
                setProperty("pdfVersion", 2, i, properties, sheet);
                setProperty("producer", 3, i, properties, sheet);
            }
            List<ValidationError> validationErrors = document.getValidationErrors();
            if (validationErrors != null && !validationErrors.isEmpty()) {
                int j = i;
                for (ValidationError error : validationErrors) {
                    sheet.setValueAt(error.getFullDescription(), 4, j++);
                }
                if (j - i > 1) {
                    for (int l = 0; l < 4; ++l) {
                        MutableCell<SpreadSheet> cell = sheet.getCellAt(l, i);
                        cell.merge(1, j - i);
                    }
                }
                i = j;
            } else {
                ++i;
            }
        }
    }

    private static void setProperty(String propertyName, int columnIndex, int rowIndex,
                                    Map<String, String> properties, Sheet sheet) {
        String property = properties.get(propertyName);
        if (property != null) {
            sheet.setValueAt(property, columnIndex, rowIndex);
        }
    }

    private static int getNonPDFA12DocumentsRowCount(List<DomainDocument> documentList) {
        if (documentList == null || documentList.isEmpty()) {
            return 0;
        }
        int res = documentList.size();
        for (DomainDocument document : documentList) {
            List<ValidationError> validationErrors = document.getValidationErrors();
            if (validationErrors != null) {
                int errorsCount = validationErrors.size();
                if (errorsCount > 1) {
                    res += errorsCount;
                }
            }
        }
        return res;
    }

    private static void fillSummary(Date documentsSince,
                                    long compliantPDFA12DocumentsCount,
                                    long odfDocumentsCount,
                                    long nonPDFA12Documents,
                                    long microsoftOfficeDocuments,
                                    long openOfficeXMLDocuments,
                                    SpreadSheet spreadSheet
    ) {
        Sheet sheet = spreadSheet.getSheet(0);
        sheet.ensureColumnCount(2);
        if (documentsSince != null) {
            sheet.setValueAt(documentsSince, 1, 0);
        } else {
            sheet.getCellAt(0, 0).clearValue();
        }
        sheet.setValueAt(compliantPDFA12DocumentsCount, 1, 2);
        sheet.setValueAt(odfDocumentsCount, 1, 3);
        sheet.setValueAt(compliantPDFA12DocumentsCount + odfDocumentsCount, 1, 4);
        sheet.setValueAt(nonPDFA12Documents, 1, 5);
        sheet.setValueAt(microsoftOfficeDocuments, 1, 6);
        sheet.setValueAt(openOfficeXMLDocuments, 1, 7);
        sheet.setValueAt(nonPDFA12Documents + microsoftOfficeDocuments + openOfficeXMLDocuments, 1, 8);
    }

    public File generateODSReport(String domain,
                                  Date documentsSince,
                                  long compliantPDFA12DocumentsCount,
                                  long odfDocumentsCount,
                                  long nonPDFA12DocumentsCount,
                                  long microsoftOfficeDocumentsCount,
                                  long openOfficeXMLDocumentsCount,
                                  List<DomainDocument> nonPDFA12Documents,
                                  List<String> microsoftOfficeDocuments,
                                  List<String> openOfficeXMLDocuments) throws IOException {

        File tempFolder = new File(odsTempFolder);
        if (!tempFolder.isDirectory() && (tempFolder.exists() || !tempFolder.mkdirs())) {
            throw new IllegalStateException("Initialization fail on obtaining ods temp folder");
        }

        File template = new File(odsTemplatePath);
        SpreadSheet spreadSheet = SpreadSheet.createFromFile(template);
        fillSummary(documentsSince,
                compliantPDFA12DocumentsCount,
                odfDocumentsCount,
                nonPDFA12DocumentsCount,
                microsoftOfficeDocumentsCount,
                openOfficeXMLDocumentsCount,
                spreadSheet);
        fillNonPDFA12Documents(nonPDFA12Documents, spreadSheet);
        fillSimpleSheet(microsoftOfficeDocuments, 2, spreadSheet);
        fillSimpleSheet(openOfficeXMLDocuments, 3, spreadSheet);

        File tempODSReport = new File(tempFolder, "logiusODSReport-" + domain + "-" + System.currentTimeMillis() + ".ods");
        spreadSheet.saveAs(tempODSReport);
        return tempODSReport;
    }
}
