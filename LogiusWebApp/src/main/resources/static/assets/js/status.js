$(function () {
    var refreshInterval = 5000;
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
    var paginationContainer = $('#pagination-crawl-job-container');
    var validationQueueContainer = $('.validation-queue');

    // Health checks
    function loadHealthChecks() {
        if (loadHealthChecksTimeout) {
            clearTimeout(loadHealthChecksTimeout);
        }
        $.ajax({
            url: "/api/admin/health/info",
            type: "GET",
            headers: {
                "Content-type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            },
            success: function (result) {
                result = result.map(function (key, value) {
                    return {'name': key['serviceName'], 'isHealthy': !key['errorNotified']};
                });
                renderHealthChecks(result);
            }
        });

        //$.get('/api/healthcheck').done(renderHealthChecks);
    }

    function renderHealthChecks(healthchecks) {
        var container = $('.health-checks');
        var loaded = container.find('.loaded');
        loaded.find('>:not(.template)').remove();

        var template = loaded.find('.template');
        $.each(healthchecks, function (name, healthcheck) {
            console.log(healthcheck)
            var element = template.clone();
            element.removeClass('template');
            element.find('.check-name').text(healthcheck['name']);

            if (healthcheck['isHealthy']) {
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
        $.ajax({
            url: "api/admin/health/info/heritrix",
            type: "GET",
            headers: {
                "Content-type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            },
            success: function (result) {
                console.log(result.engineUrl);
                heritrixEngineUrl = result.engineUrl;
                callback();
            }
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
        $.ajax({
            url: '/api/admin/health/info/active-jobs?limit=' + limit + '&start=' + start + filter,
            type: "GET",
            headers: {
                "Content-type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            },
            success: function (result, textStatus, request) {
                renderActiveJobs(result['jobs'], textStatus, request);
            }
        });
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
        return element;
    }

    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }

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
        $.ajax({
            url: '/api/admin/health/info/queue-status',
            type: "GET",
            headers: {
                "Content-type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            },
            success: function (result) {
                renderValidationQueueStatus(result);
            }
        });
    }

    function renderValidationQueueStatus(queueStatus) {
        validationQueueContainer.find('.loading').hide();
        validationQueueContainer.find('.loaded').show();

        validationQueueContainer.find('.validation-queue-size').text(queueStatus.count);

        if (queueStatus.count > 0) {
            var tbody = validationQueueContainer.find('tbody');
            tbody.find('tr:not(.template)').remove();

            var template = tbody.find('.template');
            $.each(queueStatus.topDocuments, function (index, validationJob) {
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
    loadHeritrixSettings(function () {
        loadActiveJobs();
    });
    loadValidationQueueStatus();
});
