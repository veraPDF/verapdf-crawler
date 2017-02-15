package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.verapdf.crawler.api.ValidationJobData;

import java.io.*;
import java.util.Scanner;

public class ValidationLauncher implements Runnable {
    private String jobFile;
    private String verapdfPath;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    private boolean isRunning;

    public ValidationLauncher(String jobFile, String verapdfPath) {
        this.jobFile = jobFile;
        this.verapdfPath = verapdfPath;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Scanner scanner = new Scanner(new File(jobFile));
                if(scanner.hasNext()) {
                    // Read data from the first line and remove it from file
                    String validationJobJson = scanner.nextLine();
                    FileWriter writer = new FileWriter(jobFile);
                    while(scanner.hasNextLine()) {
                        writer.write(scanner.nextLine());
                        writer.write(System.lineSeparator());
                    }
                    scanner.close();
                    writer.close();
                    ObjectMapper mapper = new ObjectMapper();
                    ValidationJobData data = mapper.readValue(validationJobJson, ValidationJobData.class);
                    // Launch verapdf CLI with pdf file as argument
                    String[] cmd = {verapdfPath, "--format", "text", data.getFilepath()};
                    ProcessBuilder pb = new ProcessBuilder().inheritIO();
                    File output = new File("output");
                    File error = new File("error");
                    output.createNewFile();
                    error.createNewFile();
                    pb.redirectOutput(output);
                    pb.redirectError(error);
                    pb.command(cmd);
                    int status = pb.start().waitFor();
                    if(pb.start().waitFor() == 0) { // Validation finished successfully
                        Scanner resultScanner = new Scanner(new File("output"));
                        FileWriter fw;
                        if(resultScanner.next().equals("PASS")) {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Valid_PDF_Report.txt", true);
                        }
                        else {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Invalid_PDF_Report.txt", true);
                        }
                        fw.write(data.getUri() + ", ");
                        fw.write(data.getTime());
                        fw.write(System.lineSeparator());
                        fw.close();
                    }
                    else {
                        Scanner errorScanner = new Scanner(new File("error"));
                        FileWriter fw = new FileWriter(data.getJobDirectory() + File.separator + "Error_Report.txt", true);
                        fw.write("The following errors occured on url " + data.getUri() + ":");
                        while(errorScanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            fw.write(line);
                            System.out.println(line);
                            fw.write(System.lineSeparator());
                        }
                        fw.close();
                    }
                    new File(data.getFilepath()).delete();
                }
                else {
                    Thread.sleep(5000);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
