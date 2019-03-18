$(function () {
    var USER_LIST_URL = "/api/user";
    var USER_CHANGE_STATUS_URL = "/api/user/update-status";
    var rowTemplate = $("#user_list").children('tbody').children('tr').clone();
    var totalPagesAmount = 1;
    var limit = 10;
    var filter = "";
    var paginationOpts = {
        totalPages: 1,
        visiblePages: 5,
        initiateStartPageClick: false,
        onPageClick: function (event, page) {
            loadAllUsers(limit, (page - 1) * limit, filter);
        }
    };

    function normalizeURL(url) {
        return url.replace(':', '%3A');
    }

    function loadAllUsers(limit, start, emailFilter, redrawPagintion) {
        var filter = "";
        if (emailFilter) {
            filter = '&emailFilter=' + emailFilter;
        }

        $.ajax({
            headers: {'Authorization': 'Bearer ' + localStorage['token']},
            url: USER_LIST_URL  + '?limit=' + limit + '&start=' + start + filter,
            type: "GET",
            success: function (result, textStatus, request) {
                $("#user_list").children('tbody').empty();
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

    function appendCrawlJob(user) {
        var row = rowTemplate.clone();
        row.find('.email').text(user.email);
        row.find('.enabled').find('input').prop("checked", user.enabled);
        row.find('.enabled').find('input').change(function () {
            $.ajax({
                headers: {'Authorization': 'Bearer ' + localStorage['token']},
                url: USER_CHANGE_STATUS_URL + "?email=" + user.email + "&status="  + $(this).is(':checked'),
                type: "PUT",
                error: function (result) {
                    reportError(result);
                }
            });
        });
        row.addClass(user.email.toLowerCase());
        $("#user_list").children('tbody').append(row);
    }


    function reportError(text) {
        if (text.status === 401 || text.status === 403) {
            window.location.href = "domains.html";
        }
        $('.domains-list-error').text(text);
    }

    $("#user_list").on("click", 'a.action-resume', function (e) {
        var link = $(this);
        var currRow = $(this).parent().parent();
        if (currRow.hasClass('disabled')) {
            return;
        }
        currRow.addClass('disabled');
        var putData = {};
        putData.email = currRow.find('.email').text();
        putData.enabled = currRow.find('.enabled').text();

        $.ajax({
            url: USER_LIST_URL + "/" + normalizeURL(link.parent().siblings().first().text()),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: {"Content-type": "application/json"},
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

    loadAllUsers(limit, 0, '', true);
});
