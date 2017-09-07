DROP TABLE IF EXISTS `crawl_job_requests`;
CREATE TABLE `crawl_job_requests` (
  `id`           VARCHAR(36) NOT NULL,
  `is_finished`  TINYINT(1)   DEFAULT '0',
  `report_email` VARCHAR(255) DEFAULT NULL,
  `crawl_since`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`)
);
DROP TABLE IF EXISTS `crawl_jobs`;
CREATE TABLE `crawl_jobs` (
  `id`          VARCHAR(36)  NOT NULL,
  `domain`      VARCHAR(255) NOT NULL,
  `job_url`     VARCHAR(255)          DEFAULT NULL,
  `start_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` DATETIME              DEFAULT NULL,
  `is_finished` TINYINT(1)            DEFAULT '0',
  `job_status`  VARCHAR(10)           DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `crawl_jobs_domain_uindex` (`domain`)
);
DROP TABLE IF EXISTS `crawl_job_requests_crawl_jobs`;
CREATE TABLE `crawl_job_requests_crawl_jobs` (
  `crawl_job_request_id` VARCHAR(36) NOT NULL,
  `crawl_job_id`         VARCHAR(36) NOT NULL,
  PRIMARY KEY (`crawl_job_request_id`, `crawl_job_id`),
  CONSTRAINT `crawl_job_requests_crawl_jobs_crawl_jobs_id_fk` FOREIGN KEY (`crawl_job_id`) REFERENCES `crawl_jobs` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `crawl_job_requests_crawl_jobs_crawl_job_requests_id_fk` FOREIGN KEY (`crawl_job_request_id`) REFERENCES `crawl_job_requests` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
DROP TABLE IF EXISTS `documents`;
CREATE TABLE `documents` (
  `document_url`    VARCHAR(255)              NOT NULL,
  `crawl_job_id`    VARCHAR(36)               NOT NULL,
  `last_modified`   DATETIME     DEFAULT NULL,
  `document_type`   VARCHAR(127) DEFAULT NULL,
  `document_status` ENUM ('open', 'not_open') NOT NULL,
  PRIMARY KEY (`document_url`),
  KEY `documents_crawl_jobs_id_fk` (`crawl_job_id`),
  CONSTRAINT `documents_crawl_jobs_id_fk` FOREIGN KEY (`crawl_job_id`) REFERENCES `crawl_jobs` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
DROP TABLE IF EXISTS `document_properties`;
CREATE TABLE `document_properties` (
  `document_url`   VARCHAR(255) NOT NULL,
  `property_name`  VARCHAR(255) NOT NULL,
  `property_value` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`document_url`, `property_name`),
  CONSTRAINT `document_properties_documents_document_url_fk` FOREIGN KEY (`document_url`) REFERENCES `documents` (`document_url`)
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
DROP TABLE IF EXISTS `documents_validation_errors`;
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
DROP TABLE IF EXISTS `pdf_validation_jobs_queue`;
CREATE TABLE `pdf_validation_jobs_queue` (
  `document_url`       VARCHAR(255)                        NOT NULL DEFAULT '',
  `job_directory`      VARCHAR(255)                        NOT NULL,
  `filepath`           VARCHAR(255)                        NOT NULL,
  `time_last_modified` DATETIME                                     DEFAULT NULL,
  `validation_status`  ENUM ('not_started', 'in_progress') NOT NULL DEFAULT 'not_started',
  PRIMARY KEY (`document_url`)
);