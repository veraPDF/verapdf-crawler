package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.validation.ValidationReportData;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.app.resources.ResourceManager;

import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final String errorReportPath;
    private final LinkedList<ValidationJobData> queue;
    private final ResourceManager resource;
    private final PDFValidator validator;

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
    }

    private boolean isRunning;
    public ValidationService(String verapdfPath, String errorReportPath, ResourceManager resource, MySqlCredentials credentials) {
        this.queue = new LinkedList<>();
        this.errorReportPath = errorReportPath;
        this.resource = resource;
        isRunning = true;
        validator = new VerapdfValidator(verapdfPath, credentials);
        try {
            manageQueue(false);
        } catch (IOException e) {
            logger.error("Error managing validation queue",e);
        }
    }

    public void addJob(ValidationJobData data) throws IOException {
        queue.add(data);
        manageQueue(true);
        logger.info("Added validation job " + data.getUri());
    }

    public Integer getQueueSize() {
        return queue.size();
    }

    @Override
    public void run() {
        while (isRunning) {
            ValidationJobData data = new ValidationJobData();
            try {
                if(!queue.isEmpty()) {
                    data = queue.remove();
                    manageQueue(true);
                    logger.info("Validating " + data.getUri());
                    ValidationReportData result;
                    try {
                        result = validator.validateAndWirteErrors(data.getFilepath(), data.errorOccurances);
                    }
                    catch (Exception e) {
                        result = new ValidationReportData();
                        result.setValid(false);
                        result.setFailedRules(0);
                        result.setPassedRules(0);
                    }
                    FileWriter fw;
                    if(result.isValid()) {
                        fw = new FileWriter(data.getJobDirectory() + File.separator + "Valid_PDF_Report.txt", true);
                        fw.write(data.getUri() + ", ");
                        fw.write(data.getTime());
                    }
                    else {
                        fw = new FileWriter(data.getJobDirectory() + File.separator + "Invalid_PDF_Report.txt", true);
                        result.setUrl(data.getUri());
                        result.setLastModified(data.getTime());
                        ObjectMapper mapper = new ObjectMapper();
                        fw.write(mapper.writeValueAsString(result));
                    }
                    fw.write(System.lineSeparator());
                    fw.close();
                }
                else {
                    Thread.sleep(60000);
                }
            } catch (Exception e) {
                logger.error("Error in validation runner",e);
            }
            finally {
                if(data != null && data.getFilepath() != null) {
                    new File(data.getFilepath()).delete();
                }
            }
        }
    }

    private synchronized void manageQueue(boolean isWrite) throws IOException {
        if(isWrite) {
            writeQueue();
        }
        else {
            loadQueue();
        }
    }

    private void writeQueue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FileWriter writer = new FileWriter(errorReportPath + "validation-jobs.txt");
        for(ValidationJobData data: queue) {
            writer.write(mapper.writeValueAsString(data));
            writer.write(System.lineSeparator());
        }
        writer.close();
    }

    private void loadQueue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Scanner scanner = new Scanner(new File(errorReportPath + "validation-jobs.txt"));
        while(scanner.hasNextLine()) {
            try {
                ValidationJobData data = mapper.readValue(scanner.nextLine(), ValidationJobData.class);
                String[] parts = data.getJobDirectory().split("/");
                data.errorOccurances = resource.getJobById(parts[parts.length - 3]).getErrorOccurances();
                queue.add(data);
            }
            catch (Exception e) {
                logger.error("Error at validation job queue loading", e);
            }
        }
        scanner.close();
    }
}