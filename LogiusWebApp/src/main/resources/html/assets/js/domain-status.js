$(function () {
    var refreshInterval = 1000;
    var heritrixEngineUrl = '';
    var crawlJob = {};
    var loadStatusTimeout;

    function normalizeURL(url){
        return url.replace(':', '%3A');
    }

    // Heritrix settings (engine URL)
    function loadHeritrixSettings(callback) {
        $.get('/api/heritrix').done(function(heritrix) {
            heritrixEngineUrl = heritrix.engineUrl;
            callback();
        });
    }

    function loadJobStatus() {
        if (loadStatusTimeout) {
            clearTimeout(loadStatusTimeout);
        }

        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain") + "/status"),
            type: "GET",
            success: function (result) {
                jobStatusLoaded(result);
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    }

    function jobStatusLoaded(jobStatus) {

        $('.status-loading').hide();
        $('.status-loaded').show();

        // Job details
        crawlJob = jobStatus.crawlJob;

        $('.main').addClass('status-' + crawlJob.status.toLowerCase(), { children: true });

        $('.domain-name span').text(crawlJob.domain);
        $('.job-date').text(crawlJob.finished ? 'Tested on ' + crawlJob.startTime + ' - ' + crawlJob.finishTime : 'Test started on ' + crawlJob.startTime);
        $('.status-text').text(crawlJob.status);

        // Crawl requests
        var requestsTbody = $('.job-requests tbody');
        requestsTbody.find('tr:not(.template)').remove();

        var requestTemplate = $('.job-requests table .template').clone().removeClass('template');
        $.each(jobStatus.crawlRequests, function(index, request) {
            var element = requestTemplate.clone();
            element.find('.id').text(request.id);
            element.find('.status').text(request.finished ? 'yes' : 'no');
            element.find('.email').text(request.emailAddress);
            requestsTbody.append(element);
        });

        // Heritrix details
        $('.heritrix .status').text(jobStatus.heritrixStatus.statusDescription);
        $('.heritrix .job-id a').text(jobStatus.crawlJob.heritrixJobId).attr('href', heritrixEngineUrl + '/job/' + jobStatus.crawlJob.heritrixJobId);
        $('.heritrix .crawled-urls .total-count').text(jobStatus.heritrixStatus.uriTotalsStatus.totalUriCount);
        $('.heritrix .crawled-urls .downloaded-count').text(jobStatus.heritrixStatus.uriTotalsStatus.downloadedUriCount);
        $('.heritrix .crawled-urls .queued-count').text(jobStatus.heritrixStatus.uriTotalsStatus.queuedUriCount);

        var heritrixLogTbody = $('.heritrix tbody');
        heritrixLogTbody.find('tr:not(.template)').remove();

        var logTemplate = $('.heritrix table .template').clone().removeClass('template');
        $.each(jobStatus.heritrixStatus.jobLogTail, function(index, message) {
            var element = logTemplate.clone();
            element.find('.message').text(message);
            heritrixLogTbody.append(element);
        });

        // Validation queue details
        $('.validation-queue .validation-queue-size').text(jobStatus.validationQueueStatus.count);

        if (jobStatus.validationQueueStatus.count > 0) {
            var queueTbody = $('.validation-queue tbody');
            queueTbody.find('tr:not(.template)').remove();

            var template = queueTbody.find('.template');
            $.each(jobStatus.validationQueueStatus.topDocuments, function(index, validationJob) {
                var element = template.clone().removeClass('template');
                element.find('.url').text(validationJob.id);
                if (validationJob.status === 'IN_PROGRESS') {
                    element.addClass('in-progress');
                }
                queueTbody.append(element);
            });

            $('.validation-queue table').show();
        } else {
            $('.validation-queue table').hide();
        }

        // Schedule refresh
        loadStatusTimeout = setTimeout(loadJobStatus, refreshInterval);
    }

    $("#action-resume").on('click', function () {
        var putData = {};//Object.assign({}, currentDomain);
        putData.domain = crawlJob.domain;
        putData.startTime = crawlJob.startTime;
        putData.finishTime = crawlJob.finishTime;

        putData.status = 'RUNNING';

        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                $('.main').removeClass('status-' + crawlJob.status.toLowerCase(), { children: true });

                // domainInfoLoaded(result);
                jobStatusLoaded(putData);


            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });

    $("#action-pause").on('click', function () {
        var putData = {};//Object.assign({}, currentDomain);
        putData.domain = crawlJob.domain;
        putData.startTime = crawlJob.startTime;
        putData.finishTime = crawlJob.finishTime;
        putData.status = 'PAUSED';

        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "PUT",
            // async:false,
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                $('.main').removeClass('status-' + crawlJob.status.toLowerCase(), { children: true });

                // domainInfoLoaded(result);   
                jobStatusLoaded(putData);

            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });

    $("#action-restart").on('click', function () {
        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "POST",
            success: function (result) {
                // domainInfoLoaded(result);
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });

    });

    loadHeritrixSettings(function() {
        loadJobStatus();
    });
});