package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.validation.*;
import org.verapdf.crawler.api.validation.error.Rule;
import org.verapdf.crawler.api.validation.error.RuleViolationError;
import org.verapdf.crawler.api.validation.error.ValidationError;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

public class ValidationErrorDAO extends AbstractDAO<ValidationError> {

    public ValidationErrorDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public ValidationError save(ValidationError error) {
        if (error instanceof RuleViolationError) {
            return save((RuleViolationError) error);
        } else {
            return saveGeneric(error);
        }
    }

    private ValidationError save(RuleViolationError error) {
        RuleViolationError existingError = findRuleErrorByRule(error.getRule());
        if (existingError != null) {
            return existingError;
        }
        return persist(error);
    }

    private ValidationError saveGeneric(ValidationError error) {
        ValidationError existingError = findErrorByDescription(error.getDescription());
        if (existingError != null) {
            return existingError;
        }
        return persist(error);
    }

    private ValidationError findErrorByDescription(String description) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ValidationError> criteriaQuery = builder.createQuery(ValidationError.class);
        Root<ValidationError> error = criteriaQuery.from(ValidationError.class);
        criteriaQuery.where(
                builder.equal(error.get(ValidationError_.description), description)
        );
        return uniqueResult(criteriaQuery);
    }

    private RuleViolationError findRuleErrorByRule(Rule rule) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<RuleViolationError> criteriaQuery = builder.createQuery(RuleViolationError.class);
        Root<RuleViolationError> error = criteriaQuery.from(RuleViolationError.class);
        Path<Rule> rulePath = error.get(RuleViolationError_.rule);
        criteriaQuery.where(builder.and(
                builder.equal(rulePath.get(Rule_.specification), rule.getSpecification()),
                builder.equal(rulePath.get(Rule_.clause), rule.getClause()),
                builder.equal(rulePath.get(Rule_.testNumber), rule.getTestNumber())
        ));
        return currentSession().createQuery(criteriaQuery).uniqueResult();
    }
}