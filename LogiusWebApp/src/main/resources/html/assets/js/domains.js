$(function () {
    function loadAllJobs(limit) {
        $.ajax({
            url: "/info/list" + '?limit=' + limit,
            type: "GET",
            success: function (result, textStatus, request) {
                $("#crawl_job_list").children('tbody').empty();
                result.forEach(function (item, i, arr) {
                    appendCrawlJob(item.crawlURL, item.startTime, item.finishTime, item.status);
                });

                setPages(parseInt(request.getResponseHeader('X-Total-Count')), limit);

            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    }

    function setPages(totalCount, limit) {
        var pagesAmount = Math.floor(totalCount / 10) + 1;
        var carrentPage = limit / 10 + 1;
        if (limit == 0) {
            $('#pagination-container').children('#previous').addClass('disabled');
        }
        if (limit == (pagesAmount - 1) * 10) {
            $('#pagination-container').children('#next').addClass('disabled');
        }

        if (pagesAmount <= 4) {
            for (var i = 0; i < 5; i++) {
                if (i < pagesAmount) {
                    // $($('#pagination-container').children()[1 + i]).children('a').text(i + 1);
                } else {
                    $($('#pagination-container').children()[1 + i]).addClass('hide');
                }
            }
        } else {
            $('#pagination-container').children('#last').children('a').text(pagesAmount);
            // $('#pagination-container').children('#first').children('a').text('1');
            // $('#pagination-container').children('#second').children('a').text('2');
            // $('#pagination-container').children('#third').children('a').text('3');
        }

    }

    function appendCrawlJob(url, start, end, status) {

        var row = $("<tr></tr>");

        var link = document.createElement("a");
        link.setAttribute("href", "domain.html?domain=" + url)
        link.append(document.createTextNode(url));
        // link.innerHTML = url;

        row.append($('<td></td>').append(link));
        row.append($('<td></td>').append(document.createTextNode(start)));
        if (status === 'running') {
            row.append($('<td></td>'));
            row.append($('<td></td>').append(document.createTextNode('Running')));
            var a = $('<a href="#" class="action d-flex flex-column"></a>');
            a.append($('<span class="material-icons"></span>').text('pause'));
            a.append($('<span class="label"></span>').text('Pause'));
            row.append($('<td class="col-actions"></td>').append(a));
            a = $('<a href="#" class="action d-flex flex-column"></a>');
            a.append($('<span class="material-icons"></span>').text('replay'));
            a.append($('<span class="label"></span>').text('Replay'));
            row.append($('<td class="col-actions"></td>').append(a));

        } else if (status === 'paused') {
            row.append($('<td></td>'));
            row.append($('<td></td>').append(document.createTextNode('Paused')));
            var a = $('<a href="#" class="action d-flex flex-column"></a>');
            a.append($('<span class="material-icons"></span>').text('play_arrow'));
            a.append($('<span class="label"></span>').text('Resume'));
            row.append($('<td class="col-actions"></td>').append(a));
            a = $('<a href="#" class="action d-flex flex-column"></a>');
            a.append($('<span class="material-icons"></span>').text('replay'));
            a.append($('<span class="label"></span>').text('Restart'));
            row.append($('<td class="col-actions"></td>').append(a));
        } else {
            row.append($('<td></td>').append(document.createTextNode(end)));
            row.append($('<td></td>').append(document.createTextNode('Finished')));
            var a = $('<a href="#" class="action d-flex flex-column"></a>');
            a.append($('<span class="material-icons"></span>').text('replay'));
            a.append($('<span class="label"></span>').text('Restart'));
            row.append($('<td class="col-actions"></td>'));
            row.append($('<td class="col-actions"></td>').append(a));
        }

        $("#crawl_job_list").children('tbody').append(row);

    }


    function reportError(text) {
        var ul = document.getElementById("crawl_url_list");
        var li = document.createElement("li");
        var link = document.createElement("p");
        link.innerHTML = "<font color=\"red\">* " + text + ".</font>"
        li.appendChild(link);
        ul.innerHTML = '';
        ul.appendChild(li);
    }

    $(".page-item .page-link").on("click", function (e) {
        $('#pagination-container').children('.active').removeClass('active');

        loadAllJobs(parseInt($(this).text() - 1) * 10);

        if ($(this).parent('li')[0].id === 'third') {
            $('#first').children('a').text(parseInt($('#first').children('a').text()) + 1);
            $('#second').children('a').text(parseInt($('#second').children('a').text()) + 1);
            $('#third').children('a').text(parseInt($('#third').children('a').text()) + 1);
            $('#second').addClass('active');
            $('#second-dots').removeClass('hide');
            $('#first-dots').addClass('hide');

            if (parseInt($('#third').children('a').text()) + 1 === parseInt($('#last').children('a').text())) {
                $('#second-dots').addClass('hide');
                $('#first-dots').removeClass('hide');
            }
            if (parseInt($('#third').children('a').text()) === parseInt($('#last').children('a').text())) {
                $('#last').addClass('hide');
                $('#first-dots').removeClass('hide');
                $('#second-dots').addClass('hide');
            }
        } else if ($(this).parent('li')[0].id === 'last') {
            $('#second-dots').addClass('hide');
            $('#first-dots').removeClass('hide');
            $('#last').addClass('active');
            $('#first').children('a').text(parseInt($('#last').children('a').text()) - 3);
            $('#second').children('a').text(parseInt($('#last').children('a').text()) - 2);
            $('#third').children('a').text(parseInt($('#last').children('a').text()) - 1);

        } else if ($(this).parent('li')[0].id === 'first') {
            if(parseInt($('#third').children('a').text()) > 2){

            } else{}

        } else if ($(this).parent('li')[0].id === 'second') {
            $('#second').addClass('active');            
        }


    });

    loadAllJobs(0);
});
