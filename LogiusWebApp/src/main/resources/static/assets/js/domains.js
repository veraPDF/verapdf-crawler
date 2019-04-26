$(function () {
    var URL = "/api/crawl-jobs";
    var rowTemplate = $("#crawl_job_list").children('tbody').children('tr').clone();
    var totalPagesAmount = 1;
    var limit = 10;
    var filter = "";
    $("#job-list-checkbox").prop('checked', true);
    var paginationOpts = {
        totalPages: 1,
        visiblePages: 5,
        initiateStartPageClick: false,
        onPageClick: function (event, page) {
            loadAllJobs(limit, (page - 1) * limit, filter);
        }
    };

    function createHeaders() {
        var headers = {"Content-type": "application/json"};
        if (getUserJobs()){
            headers['Authorization'] =  'Bearer ' + localStorage.getItem('token')
        }
        return headers
    }

    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }

    function getUserJobs(){
        return $("#job-list-checkbox").is(':checked') && localStorage['token']
    }

    function loadAllJobs(limit, start, domainFilter, redrawPagintion) {
        var filter = "";
        if (domainFilter) {
            filter = '&domainFilter=' + domainFilter;
        }

        $.ajax({
            url: URL + '?limit=' + limit + '&start=' + start + filter,
            type: "GET",
            headers: createHeaders(),
            success: function (result, textStatus, request) {
                $("#crawl_job_list").children('tbody').empty();
                if (Array.isArray(result)) {
                    result.forEach(function (crawlJob) {
                        appendCrawlJob(crawlJob);
                    });
                }
                totalPagesAmount = Math.ceil(request.getResponseHeader('X-Total-Count') / limit);
                if (totalPagesAmount > 0) {
                    paginationOpts.totalPages = totalPagesAmount;
                } else {
                    paginationOpts.totalPages = 1;
                }
                if (redrawPagintion) {
                    $('#pagination-container').twbsPagination('destroy');
                    if (paginationOpts.totalPages > 1) {
                        $('#pagination-container').twbsPagination(paginationOpts);
                    } else {
                        $('#pagination-container').hide();
                    }
                }
            },
            error: function (result) {
                reportError(result);
            }
        });
    }

    function appendCrawlJob(crawlJob) {
        var row = rowTemplate.clone();
        row.find('.domain').text(crawlJob.domain);
        row.find('.domain').attr("href", "domain.html?domain=" + crawlJob.domain + (getUserJobs()? '&isGeneralJob=false':'&isGeneralJob=true'));
        row.children('.start').text(crawlJob.startTime);
        if (crawlJob.finishTime) {
            row.children('.end').text(crawlJob.finishTime);
        }
        if (getUserJobs()){
            var cancel = row.find('.action-cancel');
            cancel.removeAttr('style');
        }
        row.children('.status').text(crawlJob.status);
        row.addClass(crawlJob.status.toLowerCase());
        $("#crawl_job_list").children('tbody').append(row);
    }


    function reportError(text) {
        $('.domains-list-error').text(text);
    }

    $("#crawl_job_list").on("click", 'a.action-resume', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        if (currRow.hasClass('disabled')) {
            return;
        }
        currRow.addClass('disabled');

        var oldStatus = currRow.find('.status').text();
        var putData = {};
        putData.domain = currRow.find('.domain').text();
        putData.startTime = currRow.children('.start').text();
        putData.finishTime = currRow.children('.end').text();
        putData.status = 'RUNNING';

        $.ajax({
            url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: createHeaders(),
            success: function (result) {
                currRow.removeClass('disabled');
                currRow.removeClass(oldStatus.toLowerCase());
                currRow.addClass(result.status.toLowerCase());
                currRow.children('.status').text(result.status);
            },
            error: function (result) {
                currRow.removeClass('disabled');
                reportError(result);
            }
        });

    });

    $("#crawl_job_list").on("click", 'a.action-cancel', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        if (currRow.hasClass('disabled')) {
            return;
        }
        currRow.addClass('disabled');
        $.ajax({
            url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "DELETE",
            headers: createHeaders(),
            success: function () {
                currRow.remove();
            },
            error: function (result) {
                reportError(result);
            }
        });
    });

    $("#crawl_job_list").on("click", 'a.action-pause', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        if (currRow.hasClass('disabled')) {
            return;
        }
        currRow.addClass('disabled');

        var oldStatus = currRow.find('.status').text();
        var putData = {};
        putData.domain = currRow.find('.domain').text();
        putData.startTime = currRow.children('.start').text();
        putData.finishTime = currRow.children('.end').text();
        putData.status = 'PAUSED';

        $.ajax({
            url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: createHeaders(),
            success: function (result) {
                currRow.removeClass('disabled');
                currRow.removeClass(oldStatus.toLowerCase());
                currRow.addClass(result.status.toLowerCase());
                currRow.children('.status').text(result.status);
            },
            error: function (result) {
                currRow.removeClass('disabled');
                reportError(result);
            }
        });
    });

    $("#job-list-checkbox").on("click", function (e) {
        console.log($("#job-list-checkbox").is(':checked'));
        loadAllJobs(limit, 0, '', true);
    });

    $("#crawl_job_list").on("click", 'a.action-restart', function (e) {
        var currRow = $(this).parent().parent();
        if (currRow.hasClass('disabled')) {
            return;
        }
        currRow.addClass('disabled');

        var oldStatus = currRow.find('.status').text();
        $.ajax({
            url: URL + "/" + normalizeURL($($(this).parent().siblings()[0]).children().text()),
            type: "POST",
            headers: createHeaders(),
            success: function (result) {
                currRow.removeClass('disabled');
                currRow.removeClass(oldStatus.toLowerCase());
                currRow.addClass(result.status.toLowerCase());
                currRow.children('.status').text(result.status);
            },
            error: function (result) {
                currRow.removeClass('disabled');
                reportError(result);
            }
        });
    });

    $("#serch-domain-action").on("click", function (e) {
        if ($("#serch-domain")[0].value) {
            filter = $("#serch-domain")[0].value;
            loadAllJobs(limit, 0, $("#serch-domain")[0].value, true);
        }
    });

    $("#serch-domain").keypress(function (e) {
        if (e.keyCode == 13 && $("#serch-domain")[0].value) {
            event.preventDefault();
            filter = $("#serch-domain")[0].value;
            loadAllJobs(limit, 0, $("#serch-domain")[0].value, true);
        }
    });

    loadAllJobs(limit, 0, '', true);
});
