package org.verapdf.crawler.logius.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * @author Maksim Bezrukov
 */


@RestController
@RequestMapping("api/validation-service")
public class ValidationServiceResource {

    private final ValidationJobDAO validationJobDAO;

    @Autowired
    public ValidationServiceResource(ValidationJobDAO validationJobDAO) {
        this.validationJobDAO = validationJobDAO;
    }

    @GetMapping("/queue-status")
    @Transactional
    public ValidationQueueStatus getQueueStatus() {
        Long count = validationJobDAO.count(null);
        List<ValidationJob> documents = validationJobDAO.getDocuments(null, 10);
        return new ValidationQueueStatus(count, documents);
    }

    @GetMapping("/result")
    public void setValidationResult(@NotNull @Valid VeraPDFValidationResult result) {
        // todo: interrupt sleep
    }
}
