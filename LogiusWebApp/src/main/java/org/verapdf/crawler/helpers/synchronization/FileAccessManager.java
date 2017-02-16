package org.verapdf.crawler.helpers.synchronization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class FileAccessManager {
    private static final FileAccessManager instance = new FileAccessManager();

    private FileAccessManager() {}

    public static FileAccessManager getInstance() {
        return instance;
    }

    // If record is empty, remove the first line and return it. Otherwise append record to the end of file and return it
    public synchronized String makeRecord(String filename, String record) throws IOException {
        if(record.equals("")) {
            StringBuilder builder = new StringBuilder();
            Scanner scanner = new Scanner(new File(filename));
            if(!scanner.hasNextLine())
                return "";
            String firstLine = scanner.nextLine();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
                builder.append(System.lineSeparator());
            }
            scanner.close();
            FileWriter writer = new FileWriter(filename);
            writer.write(builder.toString());
            writer.close();
            return firstLine;
        }
        else {
            FileWriter writer = new FileWriter(filename, true);
            writer.write(record);
            writer.write(System.lineSeparator());
            writer.close();
            return record;
        }
    }
}
