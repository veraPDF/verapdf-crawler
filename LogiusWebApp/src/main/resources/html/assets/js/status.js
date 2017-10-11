$(function () {
    var refreshInterval = 1000;
    var loadActiveDomainsListTimeout;
    var loadHealthChecksTimeout;
    var loadValidationQueueTimeout;

    var currentPage = 1;
    var limit = 10;
    var paginationOpts = {
        totalPages: 0,
        visiblePages: 5,
        initiateStartPageClick: false,
        onPageClick: function (event, page) {
            currentPage = page;
            loadActiveJobs();
        }
    };
    var invalidatePagination = true;

    var heritrixEngineUrl = '';

    var activeDomainsListContainer = $('.active-domains-list');
    var domainFilterElement = $('.domain-filter');
    var domainFilterInput = domainFilterElement.find('input');
    var paginationContainer = $('#pagination-container');
    var validationQueueContainer = $('.validation-queue');

    // Health checks
    function loadHealthChecks() {
        if (loadHealthChecksTimeout) {
            clearTimeout(loadHealthChecksTimeout);
        }
        $.get('/api/healthcheck').done(renderHealthChecks);
    }
    
    function renderHealthChecks(healthchecks) {
        var container = $('.health-checks');
        var loaded = container.find('.loaded');
        loaded.find('>:not(.template)').remove();

        var template = loaded.find('.template');
        $.each(healthchecks, function(name, healthcheck) {
            var element = template.clone();
            element.removeClass('template');
            element.find('.check-name').text(name);

            if (healthcheck['healthy']) {
                element.addClass('healthy');
                element.find('.check-status').text('healthy');
            } else {
                element.addClass('unhealthy');
                element.find('.check-status').text('unhealthy');
                element.find('.check-message').text(healthcheck.message);
            }
            loaded.append(element);
        });
        container.find('.loading').hide();
        loaded.show();

        loadHealthChecksTimeout = setTimeout(loadHealthChecks, refreshInterval);
    }

    // Heritrix settings (engine URL)
    function loadHeritrixSettings(callback) {
        $.get('/api/heritrix').done(function(heritrix) {
            heritrixEngineUrl = heritrix.engineUrl;
            callback();
        });
    }

    // Active jobs
    function loadActiveJobs() {
        if (loadActiveDomainsListTimeout) {
            clearTimeout(loadActiveDomainsListTimeout);
        }

        var start = (currentPage - 1) * limit;

        var filter = '';
        if (domainFilterInput.val() && domainFilterInput.val().trim().length > 0) {
            filter = '&domainFilter=' + domainFilterInput.val();
        }

        $.get('/api/crawl-jobs?limit=' + limit + '&start=' + start + '&finished=false' + filter).done(renderActiveJobs);
    }

    function renderActiveJobs(jobs, textStatus, request) {
        activeDomainsListContainer.find('.loading').hide();
        activeDomainsListContainer.find('.loaded').show();

        // First check and display total count
        var totalCount = request.getResponseHeader('X-Total-Count') * 1;  // * 1 will convert string to number
        activeDomainsListContainer.find('.active-jobs-count').text(totalCount);
        if (totalCount === 0) {
            activeDomainsListContainer.find('.domain-filter').hide();
            activeDomainsListContainer.find('table').hide();
            activeDomainsListContainer.find('nav').hide();

        } else {
            // If we have active jobs display related elements
            activeDomainsListContainer.find('.domain-filter').show();
            activeDomainsListContainer.find('table').show();
            activeDomainsListContainer.find('nav').show();

            // Fill the table
            var tbody = activeDomainsListContainer.find('tbody');
            tbody.find('tr:not(.template)').remove();

            var template = tbody.find('.template');
            if (Array.isArray(jobs)) {
                jobs.forEach(function (item) {
                    var row = createCrawlJobRow(item, template);
                    tbody.append(row);
                });
            }

            // Update pagination if needed (when total page count changes)
            var totalPagesAmount = totalCount > limit ? Math.ceil(totalCount / limit) : 1;
            if (totalPagesAmount !== paginationOpts.totalPages || invalidatePagination) {
                paginationOpts.totalPages = totalPagesAmount;

                paginationContainer.twbsPagination('destroy');

                if (totalPagesAmount > 1) {
                    domainFilterElement.show();
                    paginationContainer.show();
                    paginationContainer.twbsPagination(paginationOpts);
                    invalidatePagination = false;
                } else {
                    domainFilterElement.hide();
                    paginationContainer.hide();
                }
            }
        }

        // Schedule refresh
        loadActiveDomainsListTimeout = setTimeout(loadActiveJobs, refreshInterval);
    }

    function createCrawlJobRow(item, template) {
        var element = template.clone().removeClass('template');
        element.find('.domain a').text(item.domain).attr('href', 'domain-status.html?domain=' + item.domain);
        element.find('.heritrix-id a').text(item.heritrixJobId).attr('href', heritrixEngineUrl + '/job/' + item.heritrixJobId);
        element.find('.start').text(item.startTime);

        $(element).find('.status').text(item.status);
        switch (item.status) {
            case 'RUNNING':
                $(element).find('.action-resume').parent().hide();
                break;
            case 'PAUSED':
                $(element).find('.action-pause').parent().hide();
                break;
            default:
                $(element).find('.action-pause').parent().remove();
                $(element).find('.action-resume').parent().remove();
                $(element).find('.action-restart').parent().attr('colspan', 2);
        }
        return element;
    }

    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }

    activeDomainsListContainer.on("click", '.action-resume', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        var putData = {};
        putData.domain = currRow.find('.domain').text();
        putData.startTime = currRow.children('.start').text();
        putData.finishTime = currRow.children('.end').text();
        putData.status = 'RUNNING';

        $.ajax({
            url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                currRow.find('.action-pause').parent().show();
                currRow.find('.action-resume').parent().hide();
                currRow.children('.status').text(result.status);
            },
            error: function (result) { }
        });

    });

    activeDomainsListContainer.on("click", '.action-pause', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        var putData = {};
        putData.domain = currRow.find('.domain').text();
        putData.startTime = currRow.children('.start').text();
        putData.finishTime = currRow.children('.end').text();
        putData.status = 'PAUSED';

        $.ajax({
            url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                currRow.find('.action-pause').parent().hide();
                currRow.find('.action-resume').parent().show();
                currRow.children('.status').text(result.status);
            },
            error: function (result) { }
        });

    });

    activeDomainsListContainer.on("click", '.action-restart', function (e) {
        $.ajax({
            url: URL + "/" + normalizeURL($($(this).parent().siblings()[0]).children().text()),
            type: "POST",
            success: function (result) { },
            error: function (result) { }
        });
    });

    domainFilterElement.find('span').on("click", function (e) {
        currentPage = 1;
        invalidatePagination = true;
        loadActiveJobs();
    });

    domainFilterInput.keypress(function (e) {
        if (e.keyCode === 13) {
            event.preventDefault();
            domainFilterElement.find('span').click();
        }
    });

    // Validation queue
    function loadValidationQueueStatus() {
        if (loadValidationQueueTimeout) {
            clearTimeout(loadValidationQueueTimeout);
        }
        $.get('/api/validation-service/queue-status').done(renderValidationQueueStatus);
    }

    function renderValidationQueueStatus(queueStatus) {
        validationQueueContainer.find('.loading').hide();
        validationQueueContainer.find('.loaded').show();

        validationQueueContainer.find('.validation-queue-size').text(queueStatus.count);

        if (queueStatus.count > 0) {
            var tbody = validationQueueContainer.find('tbody');
            tbody.find('tr:not(.template)').remove();

            var template = tbody.find('.template');
            $.each(queueStatus.topDocuments, function(index, validationJob) {
                var element = template.clone().removeClass('template');
                element.find('.url').text(validationJob.id);
                if (validationJob.status === 'IN_PROGRESS') {
                    element.addClass('in-progress');
                }
                tbody.append(element);
            });

            validationQueueContainer.find('table').show();
        } else {
            validationQueueContainer.find('table').hide();
        }

        loadValidationQueueTimeout = setTimeout(loadValidationQueueStatus, refreshInterval);
    }

    // Start load
    loadHealthChecks();
    loadHeritrixSettings(function() {
        loadActiveJobs();
    });
    loadValidationQueueStatus();
});
