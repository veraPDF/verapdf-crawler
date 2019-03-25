DROP TABLE IF EXISTS pdf_properties_namespaces;
DROP TABLE IF EXISTS pdf_properties_xpath;
DROP TABLE IF EXISTS pdf_properties;
DROP TABLE IF EXISTS documents_validation_errors;
DROP TABLE IF EXISTS validation_errors;
DROP TABLE IF EXISTS pdf_validation_jobs_queue;
DROP TABLE IF EXISTS document_properties;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS crawl_job_requests_crawl_jobs;
DROP TABLE IF EXISTS crawl_jobs;
DROP TABLE IF EXISTS crawl_job_requests;
DROP TABLE IF EXISTS client;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE client
(
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  email        VARCHAR(128)  UNIQUE NOT NULL,
  password     VARCHAR(128)  NOT NULL,
  secret       bytea         NOT NULL,
  role         VARCHAR(128)  NOT NULL,
  enabled      BOOLEAN       DEFAULT true
);

CREATE TABLE crawl_job_requests
(
  id           VARCHAR(36)  NOT NULL,
  is_finished  BOOLEAN      DEFAULT false,
  report_email VARCHAR(255) DEFAULT NULL,
  crawl_since  DATE         DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE crawl_jobs
(
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  domain                 VARCHAR(255) NOT NULL,
  heritrix_job_id        VARCHAR(36)  NOT NULL UNIQUE ,
  job_url                VARCHAR(255) DEFAULT NULL,
  start_time             TIMESTAMP    DEFAULT NOW(),
  finish_time            TIMESTAMP    DEFAULT NULL,
  is_finished            BOOLEAN      DEFAULT FALSE,
  is_validation_enabled  BOOLEAN      DEFAULT FALSE,
  job_status             VARCHAR(10)  DEFAULT NULL,
  crawl_service          VARCHAR(10)  NOT NULL,
  user_id                UUID,
  CONSTRAINT crawl_jobs_user_id_fk FOREIGN KEY (user_id) REFERENCES client (id)
);

CREATE TABLE crawl_job_requests_crawl_jobs
(
  crawl_job_request_id VARCHAR(36)  NOT NULL,
  crawl_job_id    UUID NOT NULL,
  PRIMARY KEY (crawl_job_request_id, crawl_job_id),
  CONSTRAINT crawl_job_requests_crawl_jobs_crawl_jobs_domain_fk FOREIGN KEY (crawl_job_id) REFERENCES crawl_jobs (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT crawl_job_requests_crawl_jobs_crawl_job_requests_id_fk FOREIGN KEY (crawl_job_request_id) REFERENCES crawl_job_requests (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);


CREATE TABLE documents
(
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  document_url     VARCHAR(2048) NOT NULL,
  crawl_job_id     UUID NOT NULL,
  last_modified    TIMESTAMP     DEFAULT NULL,
  document_type    VARCHAR(127) DEFAULT NULL,
  document_status  VARCHAR(16),
  CONSTRAINT documents_crawl_jobs_domain_fk FOREIGN KEY (crawl_job_id) REFERENCES crawl_jobs (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE document_properties
(
  document_id           UUID         NOT NULL,
  property_name  VARCHAR(255) NOT NULL,
  property_value VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (document_id, property_name),
  CONSTRAINT document_properties_documents_document_url_fk FOREIGN KEY (document_id) REFERENCES documents (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE pdf_validation_jobs_queue
(
  document_id           UUID         NOT NULL,
  validation_status VARCHAR(16) NOT NULL,
  PRIMARY KEY (document_id),
  CONSTRAINT pdf_validation_jobs_queue_documents_document_url_fk FOREIGN KEY (document_id) REFERENCES documents (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE validation_errors
(
  id            SERIAL      NOT NULL,
  type          VARCHAR(32) NOT NULL DEFAULT 'GENERIC',
  specification VARCHAR(32)          DEFAULT NULL,
  clause        VARCHAR(16)          DEFAULT NULL,
  test_number   VARCHAR(4)           DEFAULT NULL,
  description   VARCHAR(2048)        DEFAULT NULL ,
  PRIMARY KEY (id),
  UNIQUE (specification, clause, test_number)
);

CREATE TABLE documents_validation_errors
(
  document_id           UUID         NOT NULL,
  error_id     BIGINT       NOT NULL DEFAULT '0',
  PRIMARY KEY (document_id, error_id),
  CONSTRAINT documents_validation_errors_documents_document_url_fk FOREIGN KEY (document_id) REFERENCES documents (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT documents_validation_errors_validation_errors_id_fk FOREIGN KEY (error_id) REFERENCES validation_errors (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE pdf_properties
(
  property_name    VARCHAR(127) NOT NULL,
  property_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
  PRIMARY KEY (property_name)
);
CREATE TABLE pdf_properties_xpath
(
  property_name VARCHAR(127) NOT NULL,
  xpath_index   BIGINT       NOT NULL DEFAULT '0',
  xpath         VARCHAR(255) NOT NULL,
  PRIMARY KEY (property_name, xpath_index)
);
CREATE TABLE pdf_properties_namespaces
(
  namespace_prefix VARCHAR(20)  NOT NULL,
  namespace_url    VARCHAR(255) NOT NULL,
  PRIMARY KEY (namespace_prefix)
);
