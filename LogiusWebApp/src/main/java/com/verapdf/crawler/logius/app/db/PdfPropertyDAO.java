package com.verapdf.crawler.logius.app.db;


import com.verapdf.crawler.logius.app.validation.settings.PdfProperty;
import com.verapdf.crawler.logius.app.validation.settings.PdfProperty_;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Repository
@Transactional
public class PdfPropertyDAO extends AbstractDAO<PdfProperty>{


    public PdfPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<PdfProperty> getEnabledPropertiesMap() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PdfProperty> criteriaQuery = builder.createQuery(PdfProperty.class);
        Root<PdfProperty> pdfProperty = criteriaQuery.from(PdfProperty.class);
        criteriaQuery.where(builder.equal(pdfProperty.get(PdfProperty_.enabled), true));
        return currentSession().createQuery(requireNonNull(criteriaQuery)).getResultList();
    }
}
