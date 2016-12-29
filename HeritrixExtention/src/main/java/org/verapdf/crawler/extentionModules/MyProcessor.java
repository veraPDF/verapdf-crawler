package org.verapdf.crawler.extentionModules;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;
import org.verapdf.core.VeraPDFException;
import org.verapdf.features.FeatureFactory;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.pdfa.PdfBoxFoundryProvider;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.ItemProcessor;
import org.verapdf.processor.ProcessorFactory;
import org.verapdf.processor.ProcessorResult;
import org.verapdf.processor.TaskType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;

public class MyProcessor extends MirrorWriterProcessor {
    @Override
    protected void innerProcess(CrawlURI curi) {
        super.innerProcess(curi);
        PdfBoxFoundryProvider.initialise();
        ItemProcessor processor = ProcessorFactory.createProcessor(
                ProcessorFactory.fromValues(
                        ValidatorFactory.defaultConfig(),
                        FeatureFactory.defaultConfig(),
                        FixerFactory.defaultConfig(),
                        EnumSet.of(TaskType.VALIDATE)));
        String baseDir = getPath().getFile().getAbsolutePath();
        String mps = (String)curi.getData().get(A_MIRROR_PATH);
        try {
            ProcessorResult res = processor.process(new File(baseDir + File.separator + mps));
            System.out.println("Document is valid: " + res.getValidationResult().isCompliant());
            FileWriter fw = new FileWriter(baseDir + File.separator + "PDFReport.txt", true);
            fw.write(curi.getURI() + "\n");
            fw.close();
            throw new VeraPDFException();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (VeraPDFException e) {
            e.printStackTrace();
        }
    }
}