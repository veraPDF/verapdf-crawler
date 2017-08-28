CREATE TABLE `crawl_jobs` (
  `id`          VARCHAR(36)        DEFAULT NULL,
  `crawl_url`   VARCHAR(255)       DEFAULT NULL,
  `job_url`     VARCHAR(255)       DEFAULT NULL,
  `start_time`  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` DATETIME           DEFAULT NULL,
  `is_finished` TINYINT(1)         DEFAULT '0',
  `status`      VARCHAR(10)        DEFAULT NULL
);

CREATE TABLE `validation_jobs` (
  `filepath`           VARCHAR(255) DEFAULT NULL,
  `job_directory`      VARCHAR(255) DEFAULT NULL,
  `file_url`           VARCHAR(255) DEFAULT NULL,
  `time_last_modified` DATETIME     DEFAULT NULL
);

CREATE TABLE `batch_crawl_jobs` (
  `id`           VARCHAR(36)  DEFAULT NULL,
  `is_finished`  TINYINT(1)   DEFAULT '0',
  `report_email` VARCHAR(255) DEFAULT NULL,
  `crawl_since`  DATETIME     DEFAULT NULL
);

CREATE TABLE `crawl_jobs_in_batch` (
  `batch_job_id` VARCHAR(36) DEFAULT NULL,
  `crawl_job_id` VARCHAR(36) DEFAULT NULL
);

CREATE TABLE `document_properties` (
  `name`         VARCHAR(255) DEFAULT NULL,
  `value`        VARCHAR(255) DEFAULT NULL,
  `document_url` VARCHAR(255) DEFAULT NULL
);

CREATE TABLE `documents` (
  `crawl_job_id`  VARCHAR(36)  DEFAULT NULL,
  `document_url`  VARCHAR(255) DEFAULT NULL,
  `last_modified` DATETIME     DEFAULT NULL,
  `document_type` VARCHAR(127) DEFAULT NULL
);

CREATE TABLE `pdf_properties` (
  `name`                VARCHAR(127) DEFAULT NULL,
  `xpath`               VARCHAR(255) DEFAULT NULL,
  `human_readable_name` VARCHAR(255) DEFAULT NULL
);

CREATE TABLE `validation_errors` (
  `specification` VARCHAR(32)      DEFAULT NULL,
  `clause`        VARCHAR(16)      DEFAULT NULL,
  `test_number`   VARCHAR(4)       DEFAULT NULL,
  `description`   VARCHAR(512)     DEFAULT NULL,
  `id`            INT(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
);

CREATE TABLE `validation_errors_in_document` (
  `document_url` VARCHAR(255) DEFAULT NULL,
  `error_id`     INT(11)      DEFAULT NULL
);
