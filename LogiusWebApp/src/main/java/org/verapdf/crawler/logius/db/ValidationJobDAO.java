package org.verapdf.crawler.logius.db;

import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.verapdf.crawler.logius.crawling.CrawlJob_;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.document.DomainDocument_;
import org.verapdf.crawler.logius.dto.ValidationJobDto;
import org.verapdf.crawler.logius.model.DocumentId;
import org.verapdf.crawler.logius.model.DocumentId_;
import org.verapdf.crawler.logius.model.User_;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.ValidationJob_;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ValidationJobDAO extends AbstractDAO<ValidationJob> {
    private final static String SELECT_VALIDATION_JOB_QUEUE  = "select *\n" +
                                                               "from ((select d.document_url, " +
                                                               "              pvjq.validation_status, " +
                                                               "              c.validation_job_priority, " +
                                                               "              crawl_jobs.start_time, " +
                                                               "              pvjq.creation_date " +
                                                               "       from crawl_jobs " +
                                                               "              join client c on crawl_jobs.user_id = c.id " +
                                                               "              join documents d on crawl_jobs.id = d.document_id " +
                                                               "              join pdf_validation_jobs_queue pvjq " +
                                                               "                   on d.document_id = pvjq.document_id and d.document_url = pvjq.document_url " +
                                                               "       where pvjq.validation_status = 'IN_PROGRESS') " +
                                                               "      union " +
                                                               "      (select distinct on (c.id) d.document_url, " +
                                                               "                                 pvjq.validation_status, " +
                                                               "                                 c.validation_job_priority, " +
                                                               "                                 crawl_jobs.start_time, " +
                                                               "                                 pvjq.creation_date " +
                                                               "       from crawl_jobs " +
                                                               "              join client c on crawl_jobs.user_id = c.id " +
                                                               "              join documents d on crawl_jobs.id = d.document_id " +
                                                               "              join pdf_validation_jobs_queue pvjq " +
                                                               "                   on d.document_id = pvjq.document_id and d.document_url = pvjq.document_url " +
                                                               "       where pvjq.validation_status = 'NOT_STARTED' " +
                                                               "       order by c.id, c.validation_job_priority, crawl_jobs.start_time, pvjq.creation_date)) as un " +
                                                               "order by array_position(array[cast('IN_PROGRESS' as varchar), cast('NOT_STARTED' as varchar)], validation_status), validation_job_priority, " +
                                                               "         start_time, creation_date ";


    public ValidationJobDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public ValidationJob save(ValidationJob validationJob) {
        return persist(validationJob);
    }

    public ValidationJob next() {
        return getValidationJobWithStatus(ValidationJob.Status.NOT_STARTED);
    }

    public List<ValidationJob> currentJobs() {
        return getValidationJobsWithStatus(ValidationJob.Status.IN_PROGRESS);
    }

    public ValidationJob current() {
        return getValidationJobWithStatus(ValidationJob.Status.IN_PROGRESS);
    }

    private List<ValidationJob> getValidationJobsWithStatus(ValidationJob.Status status) {
        return currentSession().createQuery(buildValidationJobWithStatusQuery(status)).getResultList();
    }

    private ValidationJob getValidationJobWithStatus(ValidationJob.Status status) {
        return currentSession().createQuery(buildValidationJobWithStatusQueryAndMaxPriority(status))
                .setMaxResults(1).uniqueResult();
    }

    private CriteriaQuery<ValidationJob> buildValidationJobWithStatusQuery(ValidationJob.Status status) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ValidationJob> criteriaQuery = builder.createQuery(ValidationJob.class);
        Root<ValidationJob> jobRoot = criteriaQuery.from(ValidationJob.class);
        criteriaQuery.where(builder.and(
                builder.equal(jobRoot.get(ValidationJob_.status), status),
                builder.isNotNull(jobRoot.get(ValidationJob_.documentId).get(DocumentId_.documentUrl))
        ));
        return criteriaQuery;
    }

    private CriteriaQuery<ValidationJob> buildValidationJobWithStatusQueryAndMaxPriority(ValidationJob.Status status) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ValidationJob> criteriaQuery = builder.createQuery(ValidationJob.class);
        Root<ValidationJob> jobRoot = criteriaQuery.from(ValidationJob.class);
        criteriaQuery.where(builder.and(
                builder.equal(jobRoot.get(ValidationJob_.status), status),
                builder.isNotNull(jobRoot.get(ValidationJob_.documentId).get(DocumentId_.documentUrl))
        ));
        criteriaQuery.orderBy(builder.asc(jobRoot.get(ValidationJob_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.validationJobPriority)),
                builder.asc(jobRoot.get(ValidationJob_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.startTime)),
                              builder.asc(jobRoot.get(ValidationJob_.creationDate)));
        return criteriaQuery;
    }

    public void remove(ValidationJob validationJob) {
        currentSession().delete(validationJob);
    }

    public void pause(UUID id) {
        bulkUpdateState(id, ValidationJob.Status.NOT_STARTED, ValidationJob.Status.PAUSED);
    }

    public void unpause(UUID id) {
        bulkUpdateState(id, ValidationJob.Status.PAUSED, ValidationJob.Status.NOT_STARTED);
    }

    public Long count(UUID id) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<ValidationJob> job = criteriaQuery.from(ValidationJob.class);
        criteriaQuery.select(builder.count(job));
        if (id != null) {
            criteriaQuery.where(builder.equal(job.get(ValidationJob_.document).get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.id), id));
        }
        return currentSession().createQuery(criteriaQuery).getSingleResult();
    }

    public List<ValidationJobDto> getDocuments(int limit) {
        NativeQuery query = currentSession().createSQLQuery(SELECT_VALIDATION_JOB_QUEUE).setMaxResults(limit);
        List<Object[]> rows = query.list();
        return rows.stream()
                   .map(row -> new ValidationJobDto(row[0].toString(), ValidationJob.Status.valueOf(row[1].toString())))
                   .collect(Collectors.toList());

    }

    public List<ValidationJob> getDocuments(UUID id, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ValidationJob> criteriaQuery = builder.createQuery(ValidationJob.class);
        Root<ValidationJob> job = criteriaQuery.from(ValidationJob.class);
        criteriaQuery.select(builder.construct(
                ValidationJob.class,
                job.get(ValidationJob_.documentId),
                job.get(ValidationJob_.status)
        ));
        if (id != null) {
            criteriaQuery.where(
                    builder.equal(job.get(ValidationJob_.document).get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.id), id)
            );
        }
        criteriaQuery.orderBy(
                builder.asc(job.get(ValidationJob_.status)),
                builder.asc(job.get(ValidationJob_.documentId))
        );
        Query<ValidationJob> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return list(query);
    }

    private void bulkUpdateState(UUID uuid, ValidationJob.Status oldStatus, ValidationJob.Status newStatus) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaUpdate<ValidationJob> criteriaUpdate = builder.createCriteriaUpdate(ValidationJob.class);
        Root<ValidationJob> jobRoot = criteriaUpdate.from(ValidationJob.class);

        Subquery<DocumentId> subquery = criteriaUpdate.subquery(DocumentId.class);
        Root<DomainDocument> subqueryRoot = subquery.from(DomainDocument.class);
        subquery.select(subqueryRoot.get(DomainDocument_.documentId));
        subquery.where(
                builder.equal(subqueryRoot.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.id), uuid)
        );
        criteriaUpdate.set(jobRoot.get(ValidationJob_.status), newStatus);
        criteriaUpdate.where(builder.and(
                jobRoot.get(ValidationJob_.documentId).in(subquery),
                builder.equal(jobRoot.get(ValidationJob_.status), oldStatus)
        ));
        currentSession().createQuery(criteriaUpdate).executeUpdate();
    }
}
