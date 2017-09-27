DROP TABLE IF EXISTS `crawl_job_requests_crawl_jobs`;
DROP TABLE IF EXISTS `crawl_job_requests`;
CREATE TABLE `crawl_job_requests` (
  `id`           VARCHAR(36) NOT NULL,
  `is_finished`  TINYINT(1)   DEFAULT '0',
  `report_email` VARCHAR(255) DEFAULT NULL,
  `crawl_since`  DATE     DEFAULT NULL,
  PRIMARY KEY (`id`)
);
DROP TABLE IF EXISTS `document_properties`;
DROP TABLE IF EXISTS `documents_validation_errors`;
DROP TABLE IF EXISTS `documents`;
DROP TABLE IF EXISTS `pdf_validation_jobs_queue`;
DROP TABLE IF EXISTS `crawl_jobs`;
CREATE TABLE `crawl_jobs` (
  `domain`          VARCHAR(255) NOT NULL,
  `heritrix_job_id` VARCHAR(36)  NOT NULL,
  `job_url`         VARCHAR(255)          DEFAULT NULL,
  `start_time`      DATETIME              DEFAULT NULL,
  `finish_time`     DATETIME              DEFAULT NULL,
  `is_finished`     TINYINT(1)            DEFAULT '0',
  `job_status`      VARCHAR(10)           DEFAULT NULL,
  PRIMARY KEY (`domain`),
  UNIQUE KEY `crawl_jobs_domain_uindex` (`heritrix_job_id`)
);
DROP TRIGGER IF EXISTS crawl_jobs_B4_INSERT;
CREATE TRIGGER crawl_jobs_B4_INSERT BEFORE INSERT ON `crawl_jobs`
    FOR EACH ROW SET NEW.start_time = IFNULL(NEW.start_time, NOW());

CREATE TABLE `crawl_job_requests_crawl_jobs` (
  `crawl_job_request_id` VARCHAR(36)  NOT NULL,
  `crawl_job_domain`     VARCHAR(255) NOT NULL,
  PRIMARY KEY (`crawl_job_request_id`, `crawl_job_domain`),
  CONSTRAINT `crawl_job_requests_crawl_jobs_crawl_jobs_domain_fk` FOREIGN KEY (`crawl_job_domain`) REFERENCES `crawl_jobs` (`domain`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `crawl_job_requests_crawl_jobs_crawl_job_requests_id_fk` FOREIGN KEY (`crawl_job_request_id`) REFERENCES `crawl_job_requests` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE `documents` (
  `document_url`     VARCHAR(255)              NOT NULL,
  `crawl_job_domain` VARCHAR(255)              NOT NULL,
  `last_modified`    DATETIME     DEFAULT NULL,
  `document_type`    VARCHAR(127) DEFAULT NULL,
  `document_status`  ENUM ('open', 'not_open') NOT NULL,
  PRIMARY KEY (`document_url`),
  CONSTRAINT `documents_crawl_jobs_domain_fk` FOREIGN KEY (`crawl_job_domain`) REFERENCES `crawl_jobs` (`domain`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE `document_properties` (
  `document_url`   VARCHAR(255) NOT NULL,
  `property_name`  VARCHAR(255) NOT NULL,
  `property_value` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`document_url`, `property_name`),
  CONSTRAINT `document_properties_documents_document_url_fk` FOREIGN KEY (`document_url`) REFERENCES `documents` (`document_url`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE TABLE `pdf_validation_jobs_queue` (
  `document_url`       VARCHAR(255)                        NOT NULL DEFAULT '',
  `heritrix_job_id`    VARCHAR(36)                         NOT NULL,
  `job_directory`      VARCHAR(255)                        NOT NULL,
  `filepath`           VARCHAR(255)                        NOT NULL,
  `time_last_modified` DATETIME                                     DEFAULT NULL,
  `validation_status`  ENUM ('not_started', 'in_progress') NOT NULL DEFAULT 'not_started',
  PRIMARY KEY (`document_url`),
  CONSTRAINT `pdf_validation_jobs_queue_crawl_jobs_heritrix_job_id_fk` FOREIGN KEY (`heritrix_job_id`) REFERENCES `crawl_jobs` (`heritrix_job_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
DROP TABLE IF EXISTS `validation_errors`;
CREATE TABLE `validation_errors` (
  `id`            INT(11) NOT NULL AUTO_INCREMENT,
  `specification` VARCHAR(32)      DEFAULT NULL,
  `clause`        VARCHAR(16)      DEFAULT NULL,
  `test_number`   VARCHAR(4)       DEFAULT NULL,
  `description`   VARCHAR(512)     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `validation_errors_specification_clause_test_number_pk` (`specification`, `clause`, `test_number`)
);
CREATE TABLE `documents_validation_errors` (
  `document_url` VARCHAR(255) NOT NULL DEFAULT '',
  `error_id`     INT(11)      NOT NULL DEFAULT '0',
  PRIMARY KEY (`document_url`, `error_id`),
  CONSTRAINT `documents_validation_errors_documents_document_url_fk` FOREIGN KEY (`document_url`) REFERENCES `documents` (`document_url`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `documents_validation_errors_validation_errors_id_fk` FOREIGN KEY (`error_id`) REFERENCES `validation_errors` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
DROP TABLE IF EXISTS `pdf_properties`;
CREATE TABLE `pdf_properties` (
  `property_name` VARCHAR(127) NOT NULL,
  `xpath_index`   INT(11)      NOT NULL DEFAULT '0',
  `xpath`         VARCHAR(255) NOT NULL,
  PRIMARY KEY (`property_name`, `xpath_index`)
);
DROP TABLE IF EXISTS `pdf_properties_namespaces`;
CREATE TABLE `pdf_properties_namespaces` (
  `namespace_prefix` VARCHAR(20)  NOT NULL,
  `namespace_url`    VARCHAR(255) NOT NULL,
  PRIMARY KEY (`namespace_prefix`)
);
