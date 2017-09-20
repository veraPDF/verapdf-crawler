var URL = "/api/crawl-requests";

$(document).ready(function () {
    // $("#date_input").value = "01-01-2015";
    $("input:button").click(main);
    $('[data-toggle="tooltip"]').tooltip();


    var errorDomainMessage = 'Incorrect Domain';
    var errorMailMessage = 'Incorrect Email';
    var errorDateMessage = 'Incorrect Date';

    var domainInput = $('#urlinput').tooltip({
        trigger: 'manual',
        template: '<div class="tooltip error" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorDomainMessage;
        }
    });

    var emailInput = $('#email_input').tooltip({
        trigger: 'manual',
        template: '<div class="tooltip error" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorMailMessage;
        }
    });

    var dateInput = $('#date_input').tooltip({
        trigger: 'manual',
        template: '<div class="tooltip error" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorDateMessage;
        }
    })

    var picker = new Pikaday(
        {
            field: document.getElementById('date_input'),
            firstDay: 1,
            minDate: new Date(2000, 12, 31),
            maxDate: new Date(2020, 12, 31),
            yearRange: [2000, 2020],
            showTime: false,
            format: 'YYYY-MM-DD',
            disableDayFn: function (date) {
                var currDate = new Date();
                return currDate.valueOf() < date.valueOf();
            }
        });
});



function main() {
    $("input:button").attr("disabled", true);

    if ($("#email_input")[0].value) {
        var regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if (!regexp.test($("#email_input")[0].value)) {
            $("input:button").attr("disabled", false);
            $('#email_input').tooltip('show');
            return;
        }
    }

    var dateRegExp = /^\d{4}-\d{2}-\d{2}$/
    if ($("#date_input")[0].value && !dateRegExp.test($("#date_input")[0].value)) {
        $("input:button").attr("disabled", false);
        $('#date_input').tooltip('show');
        return;
    }

    if (!$("#urlinput")[0].value) {
        $("input:button").attr("disabled", false);
        $('#urlinput').tooltip("show");
        return;
    }
    // var crawlUrlList = $("#urlinput")[0].value.split(', ');

    var postData = {};
    postData.domains = crawlUrlList;
    if ($("#email_input")[0].value) {
        postData.emailAddress = $("#email_input")[0].value;
    }
    if ($("#date_input")[0].value) {
        postData.crawlSinceTime = $("#date_input")[0].value;
    }

    $.ajax({
        url: URL,
        type: "POST",
        data: JSON.stringify(postData),
        async: false,
        headers: {
            "content-type": "application/json"
        },
        success: function (result) {
            window.location.href = "domains.html";
        },
        error: function (result) {
            reportError(result.responseJSON.message);
            $("input:button").attr("disabled", false);
        }
    });
}

function reportError(text) {
    var container = $(".error-message-container");
    container.text(text);
    container.css({"color": "red"});
    // var ul = document.getElementById("crawl_url_list");
    // var li = document.createElement("li");
    // var link = document.createElement("p");
    // link.innerHTML = "<font color=\"red\">* " + text + ".</font>"
    // li.appendChild(link);
    // ul.innerHTML = '';
    // ul.appendChild(li);
}

function keyListener(e) {
    $('#urlinput').tooltip("hide");
    $('#date_input').tooltip("hide");
    $('#email_input').tooltip("hide");
    if (e.keyCode == 13) {
        main();
    }
}
