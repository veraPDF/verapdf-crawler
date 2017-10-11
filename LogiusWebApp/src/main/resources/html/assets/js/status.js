$(function () {
    var URL = "/api/crawl-jobs";
    var row = $(".active-domains-list").children('tbody').children('tr').clone();
    var totalPagesAmount = 1;
    var limit = 10;
    var filter = "";
    var paginationOpts = {
        totalPages: 1,
        visiblePages: 5,
        initiateStartPageClick: false,
        onPageClick: function (event, page) {
            loadAllJobs(limit, (page - 1) * limit, filter);
        }
    };

    // Health checks
    function loadHealthChecks() {
        $.get('/api/healthcheck').done(renderHealthChecks);
    }
    
    function renderHealthChecks(healthchecks) {
        var container = $('.health-checks');
        var template = container.find('.template');
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
            container.append(element);
        });
        container.find('.loading').hide();
    }
    loadHealthChecks();
    

    // Active jobs
    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }
    function loadAllJobs(limit, start, domainFilter, redrawPagintion) {
        var filter = "";
        if (domainFilter) {
            filter = '&domainFilter=' + domainFilter;
        }
        $.ajax({
            url: URL + '?limit=' + limit + '&start=' + start + filter,
            type: "GET",
            success: function (result, textStatus, request) {
                $(".active-domains-list").children('tbody').empty();
                if (Array.isArray(result)) {
                    result.forEach(function (item, i, arr) {
                        appendCrawlJob(item.domain, item.startTime, item.finishTime, item.status, row[0]);
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
                    $('#pagination-container').twbsPagination(paginationOpts);
                }
            },
            error: function (result) {
            }
        });
    }

    function appendCrawlJob(url, start, end, status, row) {
        var row1 = $(row).clone();
        row1.find('.domain').text(url);
        row1.find('.domain').attr("href", "domain.html?domain=" + url);
        row1.children('.start').text(start);
        if (status === 'NEW') {
            row1.children('.end').text('');
            $(row1).children('.status').text('New');
            $(row1).find('.empty-action').parent().addClass('hide');
            $(row1).find('.action-pause').parent().removeClass('hide');
            $(row1).find('.action-resume').parent().addClass('hide');
            $(row1).find('.action-restart').parent().removeClass('hide');

        }
        if (status === 'RUNNING') {
            row1.children('.end').text('');
            $(row1).children('.status').text('Running');
            $(row1).find('.empty-action').parent().addClass('hide');
            $(row1).find('.action-pause').parent().removeClass('hide');
            $(row1).find('.action-resume').parent().addClass('hide');
            $(row1).find('.action-restart').parent().removeClass('hide');

        } else if (status === 'PAUSED') {
            row1.children('.end').text('');
            $(row1).children('.status').text('Paused');
            $(row1).find('.empty-action').parent().addClass('hide');
            $(row1).find('.action-pause').parent().addClass('hide');
            $(row1).find('.action-resume').parent().removeClass('hide');
            $(row1).find('.action-restart').parent().removeClass('hide');

        } else if (status === 'FAILED') {
            row1.children('.end').text(end);
            $(row1).children('.status').text('Failed');
            $(row1).find('.empty-action').parent().removeClass('hide');
            $(row1).find('.action-pause').parent().addClass('hide');
            $(row1).find('.action-resume').parent().addClass('hide');
            $(row1).find('.action-restart').parent().removeClass('hide');
        } else if (status === 'FINISHED') {
            row1.children('.end').text(end);
            $(row1).children('.status').text('Finished');
            $(row1).find('.empty-action').parent().removeClass('hide');
            $(row1).find('.action-pause').parent().addClass('hide');
            $(row1).find('.action-resume').parent().addClass('hide');
            $(row1).find('.action-restart').parent().removeClass('hide');
        }

        $(".active-domains-list").children('tbody').append(row1);

    }


    // function reportError(text) {
    //     var ul = document.getElementById("crawl_url_list");
    //     var li = document.createElement("li");
    //     var link = document.createElement("p");
    //     link.innerHTML = "<font color=\"red\">* " + text + ".</font>"
    //     li.appendChild(link);
    //     ul.innerHTML = '';
    //     ul.appendChild(li);
    // }


    // $('#pagination-container').twbsPagination(paginationOpts);


    $(".active-domains-list").on("click", '.action-resume', function (e) {
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
                currRow.find('.action-pause').parent().removeClass('hide');
                currRow.find('.action-resume').parent().addClass('hide');
                currRow.children('.status').text('Running');
            },
            error: function (result) { }
        });

    });

    $(".active-domains-list").on("click", '.action-pause', function (e) {
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
                currRow.find('.action-pause').parent().addClass('hide');
                currRow.find('.action-resume').parent().removeClass('hide');
                currRow.children('.status').text('Paused');
            },
            error: function (result) { }
        });

    });

    $(".active-domains-list").on("click", '.action-restart', function (e) {
        $.ajax({
            url: URL + "/" + normalizeURL($($(this).parent().siblings()[0]).children().text()),
            type: "POST",
            success: function (result) { },
            error: function (result) { }
        });
    });

    $("#serch-domain-action").on("click", function (e) {
        if ($("#serch-domain")[0].value) {
            filter = $("#serch-domain")[0].value;
            loadAllJobs(limit, 0, $("#serch-domain")[0].value, true);
        }
    });

    $("#serch-domain").keypress(function (e) {
        if (e.keyCode === 13 && $("#serch-domain")[0].value) {
            event.preventDefault();
            filter = $("#serch-domain")[0].value;
            loadAllJobs(limit, 0, $("#serch-domain")[0].value, true);
        }
    });

    loadAllJobs(limit, 0, '', true);
});
