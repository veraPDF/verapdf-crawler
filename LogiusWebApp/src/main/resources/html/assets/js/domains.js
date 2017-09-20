$(function () {
    var URL = "/api/crawl-jobs"
    var totalPagesAmount = 1;
    var limit = 3;
    var paginationOpts = {
        totalPages: 1,
        visiblePages: 5,
        initiateStartPageClick: false,
        onPageClick: function (event, page) {
            // $('#page-content').text('Page ' + page);
            // var start = 10;//
            loadAllJobs(limit, (page-1)*limit);
            // console.log('click');
        }
    };
    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }
    function loadAllJobs(limit, start, domainFilter) {
        var filter = "";
        if (domainFilter) {
            filter = '&domainFilter=' + domainFilter;
        }
        $.ajax({
            url: URL + '?limit=' + limit + '&start=' + start + filter,
            type: "GET",
            success: function (result, textStatus, request) {
                var row = $("#crawl_job_list").children('tbody').children('tr').clone();
                $("#crawl_job_list").children('tbody').empty();
                if (Array.isArray(result)) {
                    result.forEach(function (item, i, arr) {
                        appendCrawlJob(item.domain, item.startTime, item.finishTime, item.status, row[0]);
                    });
                }
                totalPagesAmount = Math.ceil(request.getResponseHeader('X-Total-Count') / limit);
                paginationOpts.totalPages = totalPagesAmount;
                // $('#pagination-container').twbsPagination('destroy');
                $('#pagination-container').twbsPagination(paginationOpts);
            },
            error: function (result) {
            }
        });
    }



    function appendCrawlJob(url, start, end, status, row) {
        var row1 = $(row).clone();
        row1.find('#domain').text(url)
        row1.find('#domain')[0].setAttribute("href", "domain.html?domain=" + url);
        row1.children('#start').text(start)
        if (status === 'running') {
            row1.children('#end').text('')
            $(row1).children('#status').text('Running');
            $(row1).find('#action1 span:first-child').text('pause');
            $(row1).find('#action1 span:last-child').text('Pause');
            $(row1).find('#action2 span:first-child').text('replay');
            $(row1).find('#action2 span:last-child').text('Replay');

        } else if (status === 'paused') {
            row1.children('#end').text('')
            $(row1).children('#status').text('Paused');
            $(row1).find('#action1 span:first-child').text('play_arrow');
            $(row1).find('#action1 span:last-child').text('Resume');
            $(row1).find('#action2 span:first-child').text('replay');
            $(row1).find('#action2 span:last-child').text('Restart');

        } else {
            row1.children('#end').text(end)
            $(row1).children('#status').text('Finished');
            $(row1).find('#action2 span:first-child').text('replay');
            $(row1).find('#action2 span:last-child').text('Restart');
        }

        $("#crawl_job_list").children('tbody').append(row1);

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


    $("#crawl_job_list").on("click", '#action1', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        var putData = {};
        putData.domain = currRow.find('#domain').text();
        putData.startTime = currRow.children('#start').text();
        putData.finishTime = currRow.children('#end').text();

        if ($(this).children().last().text() === 'Pause') {
            putData.status = 'paused';

            $.ajax({
                url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
                type: "PUT",
                data: JSON.stringify(putData),
                headers: { "Content-type": "application/json" },
                success: function (result) {
                    link.children().first().text("play_arrow");
                    link.children().last().text("Resume");

                },
                error: function (result) { }
            });
        } else if ($(this).children().last().text() === 'Resume') {
            putData.status = 'running';

            $.ajax({
                url: URL + "/" + normalizeURL(link.parent().siblings().first().text()),
                type: "PUT",
                data: JSON.stringify(putData),
                headers: { "Content-type": "application/json" },
                success: function (result) {
                    link.children().first().text("pause");
                    link.children().last().text("Pause");

                },
                error: function (result) { }
            });
        }

    })

    $("#crawl_job_list").on("click", '#action2', function (e) {
        $.ajax({
            url: URL + "/" + normalizeURL($($(this).parent().siblings()[0]).children().text()),
            type: "POST",
            success: function (result) { },
            error: function (result) { }
        });
    })

    loadAllJobs(limit, 0);
});
