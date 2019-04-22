CREATE VIEW select_jobs_in_queue as

  (select document_url, validation_status
   from pdf_validation_jobs_queue q
          join crawl_jobs on q.document_id = crawl_jobs.id
          join client c on crawl_jobs.user_id = c.id
   where (q.document_id, q.creation_date) in
         (select queue.document_id, min(queue.creation_date)
          from pdf_validation_jobs_queue queue
                 join crawl_jobs j on (j.user_id, j.start_time) in
                                      (select user_id, min(crawl_jobs.start_time) as time
                                       from crawl_jobs
                                       where crawl_jobs.job_status != 'FINISHED'
                                         and crawl_jobs.id in (select q.document_id from pdf_validation_jobs_queue)
                                       group by user_id) and queue.document_id = j.id and
                                      queue.validation_status = 'NOT_STARTED'

          group by queue.document_id)
   order by c.validation_job_priority asc, crawl_jobs.start_time)
  union
  (select document_url, validation_status
   from pdf_validation_jobs_queue q
          join crawl_jobs on q.document_id = crawl_jobs.id
          join client c on crawl_jobs.user_id = c.id
   where (q.document_id, q.document_url) in
         (select queue.document_id, queue.document_url
          from pdf_validation_jobs_queue queue
                 join crawl_jobs j on (j.user_id, j.start_time) in
                                      (select user_id, min(crawl_jobs.start_time) as time
                                       from crawl_jobs
                                       where crawl_jobs.job_status != 'FINISHED'
                                         and crawl_jobs.id in (select q.document_id from pdf_validation_jobs_queue)
                                       group by user_id) and queue.document_id = j.id and
                                      queue.validation_status = 'IN_PROGRESS'
          group by queue.document_id, queue.document_url)
   order by c.validation_job_priority asc, crawl_jobs.start_time)
  order by validation_status
  limit 10;