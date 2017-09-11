$(function () {
    var totalPagesAmount = 0;
    function loadAllJobs(limit) {
        $.ajax({
            url: "/info/list" + '?limit=' + limit,
            type: "GET",
            success: function (result, textStatus, request) {
                var row = $("#crawl_job_list").children('tbody').children('tr').clone();
                $("#crawl_job_list").children('tbody').empty();
                result.forEach(function (item, i, arr) {
                    appendCrawlJob(item.crawlURL, item.startTime, item.finishTime, item.status, row[0]);
                });
                totalPagesAmount = parseInt(request.getResponseHeader('X-Total-Count'));
                totalPagesAmount = Math.floor(request.getResponseHeader('X-Total-Count') / 10) + 1;

                setPages(request.getResponseHeader('X-Total-Count'), limit);

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
        } else if (limit == (pagesAmount - 1) * 10) {
            $('#pagination-container').children('#next').addClass('disabled');
        } else {
            $('#pagination-container').children('#previous').removeClass('disabled');
            $('#pagination-container').children('#next').removeClass('disabled');
        }

        if (pagesAmount < 6) {
            for (var i = 0; i < 5; i++) {
                if (i < pagesAmount) {
                    // $($('#pagination-container').children()[1 + i]).children('a').text(i + 1);
                } else {
                    $($('#pagination-container').children()[2 + i]).addClass('hide');
                }
            }
        } else {
            // $('#pagination-container').children('#last').children('a').text(pagesAmount);
            // $('#pagination-container').children('#first').children('a').text('1');
            // $('#pagination-container').children('#second').children('a').text('2');
            // $('#pagination-container').children('#third').children('a').text('3');
        }

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

        if ($(this).parent('li')[0].id === 'fifth') {
            $('#pagination-container').children('.active').removeClass('active');
            loadAllJobs(parseInt($(this).text() - 1) * 10);
            var diff = totalPagesAmount - parseInt($('#fifth').children('a').text());

            if (diff < 4) {
                $('#first').children('a').text(parseInt($('#first').children('a').text()) + diff);
                $('#second').children('a').text(parseInt($('#second').children('a').text()) + diff);
                $('#third').children('a').text(parseInt($('#third').children('a').text()) + diff);
                $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) + diff);
                $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) + diff);
            }

            switch (diff) {
                case 0:
                    $('#fifth').addClass('active');
                    break;
                case 1:
                    $('#fourth').addClass('active');
                    break;

                case 2:
                    $('#third').addClass('active');
                    break;

                case 3:
                    $('#second').addClass('active');
                    break;

                default:
                    $('#first').children('a').text(parseInt($('#first').children('a').text()) + 4);
                    $('#second').children('a').text(parseInt($('#second').children('a').text()) + 4);
                    $('#third').children('a').text(parseInt($('#third').children('a').text()) + 4);
                    $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) + 4);
                    $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) + 4);
                    $('#first').addClass('active');
                    break;
            }

        } else if ($(this).parent('li')[0].id === 'first') {
            $('#pagination-container').children('.active').removeClass('active');
            loadAllJobs(parseInt($(this).text() - 1) * 10);

            var diff = parseInt($('#first').children('a').text()) - 1;

            if (diff < 4) {
                $('#first').children('a').text(parseInt($('#first').children('a').text()) - diff);
                $('#second').children('a').text(parseInt($('#second').children('a').text()) - diff);
                $('#third').children('a').text(parseInt($('#third').children('a').text()) - diff);
                $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) - diff);
                $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) - diff);
            }

            switch (diff) {
                case 0:
                    $('#first').addClass('active');
                    break;
                case 1:
                    $('#second').addClass('active');
                    break;

                case 2:
                    $('#third').addClass('active');
                    break;

                case 3:
                    $('#fourth').addClass('active');
                    break;

                default:
                    $('#first').children('a').text(parseInt($('#first').children('a').text()) - 4);
                    $('#second').children('a').text(parseInt($('#second').children('a').text()) - 4);
                    $('#third').children('a').text(parseInt($('#third').children('a').text()) - 4);
                    $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) - 4);
                    $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) - 4);
                    $('#fifth').addClass('active');
                    break;
            }

        } else if ($(this).parent('li')[0].id === 'previous') {
            if ($('#pagination-container').children('.active')[0].id === "first") {
                $('#first').children('a').text(parseInt($('#first').children('a').text()) - 1);
                $('#second').children('a').text(parseInt($('#second').children('a').text()) - 1);
                $('#third').children('a').text(parseInt($('#third').children('a').text()) - 1);
                $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) - 1);
                $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) - 1);
            } else {
                $('#pagination-container').children('.active').removeClass('active').prev().addClass('active')
            }

            loadAllJobs(parseInt($('#pagination-container').children('.active').children('a').text() - 1) * 10);

        } else if ($(this).parent('li')[0].id === 'next') {
            if ($('#pagination-container').children('.active')[0].id === "fifth") {
                $('#first').children('a').text(parseInt($('#first').children('a').text()) + 1);
                $('#second').children('a').text(parseInt($('#second').children('a').text()) + 1);
                $('#third').children('a').text(parseInt($('#third').children('a').text()) + 1);
                $('#fourth').children('a').text(parseInt($('#fourth').children('a').text()) + 1);
                $('#fifth').children('a').text(parseInt($('#fifth').children('a').text()) + 1);
            } else {
                $('#pagination-container').children('.active').removeClass('active').next().addClass('active')
            }

            loadAllJobs(parseInt($('#pagination-container').children('.active').children('a').text() - 1) * 10);

        } else if ($(this).parent('li')[0].id === 'start') {
            $('#pagination-container').children('.active').removeClass('active');
            loadAllJobs(0);

            $('#first').children('a').text(1);
            $('#second').children('a').text(2);
            $('#third').children('a').text(3);
            $('#fourth').children('a').text(4);
            $('#fifth').children('a').text(5);
            $('#first').addClass('active');

        } else if ($(this).parent('li')[0].id === 'end') {
            $('#pagination-container').children('.active').removeClass('active');
            loadAllJobs(parseInt(totalPagesAmount - 1) * 10);

            $('#first').children('a').text(totalPagesAmount - 4);
            $('#second').children('a').text(totalPagesAmount - 3);
            $('#third').children('a').text(totalPagesAmount - 2);
            $('#fourth').children('a').text(totalPagesAmount - 1);
            $('#fifth').children('a').text(totalPagesAmount);
            $('#fifth').addClass('active');

        } else {
            $('#pagination-container').children('.active').removeClass('active');
            loadAllJobs(parseInt($(this).text() - 1) * 10);

            $(this).parent('li').addClass('active');
        }

    });

    $("#crawl_job_list").on("click", '#action1', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        var putData = {}; 
        putData.crawlURL = currRow.find('#domain').text();
        putData.startTime = currRow.children('#start').text();
        putData.finishTime = currRow.children('#end').text();
        
        if($(this).children().last().text() === 'Pause'){
            putData.status = 'paused';
            
            $.ajax({
                url: "/crawl-jobs/" + link.parent().siblings().first().text() + "/requests",
                type: "PUT",
                // async:false,
                data: JSON.stringify(putData),
                headers: { "Content-type": "application/json" },
                success: function (result) {
                    link.children().first().text("play_arrow");
                    link.children().last().text("Resume");
    
                },
                error: function (result) {
                    // reportError("Error on job loading");
                }
            });
        }else if ($(this).children().last().text() === 'Resume'){
            putData.status = 'running';            

            $.ajax({
                url: "/crawl-jobs/" + link.parent().siblings().first().text() + "/requests",
                type: "PUT",
                // async:false,
                data: JSON.stringify(putData),
                headers: { "Content-type": "application/json" },
                success: function (result) {
                    link.children().first().text("pause");                    
                    link.children().last().text("Pause"); 
    
                },
                error: function (result) {
                    // reportError("Error on job loading");
                }
            });
        }
        
    })

    $("#crawl_job_list").on("click", '#action2', function (e) {
        $.ajax({
            url: "/restart/" + $($(this).parent().siblings()[0]).children().text(),
            type: "POST",
            success: function (result) {                
            },
            error: function (result) {
            }
        });
    })

    loadAllJobs(0);
});
