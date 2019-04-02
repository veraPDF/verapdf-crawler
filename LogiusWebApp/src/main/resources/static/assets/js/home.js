$(document).ready(function () {
    $('#date_input').val('2015-01-01');
    $("input:button").click(main);
    $('[data-toggle="tooltip"]').tooltip();


    var errorDomainMessage = 'Incorrect Domain';
    var errorMailMessage = 'Incorrect Email';
    var errorDateMessage = 'Incorrect Date';

    var domainInput = $('#urlinput').tooltip({
        trigger: 'manual',
        placement: 'right',
        template: '<div class="tooltip error plasement right" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorDomainMessage;
        }
    });

    var emailInput = $('#email_input').tooltip({
        trigger: 'manual',
        placement: 'right',
        template: '<div class="tooltip error plasement right" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorMailMessage;
        }
    });

    var dateInput = $('#date_input').tooltip({
        trigger: 'manual',
        placement: 'right',
        template: '<div class="tooltip error plasement right" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorDateMessage;
        }
    });

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
    $('#date_input').tooltip('hide');
    $('#email_input').tooltip('hide');
    $('#urlinput').tooltip("hide");
    console.log($("#validation-required").is(':checked'));
    var validForm = true;

    // Read and validate domains
    if (!$("#urlinput")[0].value) {
        $("input:button").attr("disabled", false);
        $('#urlinput').tooltip("show");
        validForm = false;
    }
    var domainList = $("#urlinput")[0].value.split(', ');
    var crawlJobs = [];
    $.each(domainList, function (i, domain) {
        crawlJobs.push({
            domain: domain
        });
    });

    // Read and validate email
    var email = $("#email_input")[0].value;
    if (email) {
        var regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if (!regexp.test(email)) {
            $("input:button").attr("disabled", false);
            $('#email_input').tooltip('show');
            validForm = false;
        }
    }

    // Read and validate crawl since date
    var crawlSinceTime = $("#date_input")[0].value;
    var dateRegExp = /^\d{4}-\d{2}-\d{2}$/;
    if (crawlSinceTime && !dateRegExp.test(crawlSinceTime)) {
        $("input:button").attr("disabled", false);
        $('#date_input').tooltip('show');
        validForm = false;
    }

    var URL = "/api/crawl-requests";

    // If there are validate errors do nothing
    if (!validForm) {
        return;
    }

    // Otherwise POST crawl request
    var postData = {};
    postData.crawlJobs = crawlJobs;
    if (email) {
        postData.emailAddress = email;
    }
    if (crawlSinceTime) {
        postData.crawlSinceTime = crawlSinceTime;
    }
    var params = {
        headers: {},
        url: URL + "?isValidationRequired=" + $("#validation-required").is(':checked') + "&crawlService=" + ($("#bing-crawl-service").is(':checked') ? 'HERITRIX' : 'BING'),
        type: "POST",
        data: JSON.stringify(postData),
        async: false,
        success: function (result) {
            window.location.href = "domains.html";
        },
        error: function (result) {
            reportError(result.responseJSON.message);
            $("input:button").attr("disabled", false);
        }
    };
    if (localStorage['token']) {
        params['headers']['Authorization'] = 'Bearer ' + localStorage['token'];
    }
    params['headers']['content-type'] = 'application/json';

    $.ajax(params);
}

function reportError(text) {
    var container = $(".error-message-container");
    container.text(text);
    container.css({"color": "red"});
}

function keyListener(e) {
    $('#urlinput').tooltip("hide");
    $('#date_input').tooltip("hide");
    $('#email_input').tooltip("hide");
    if (e.keyCode == 13) {
        main();
    }

}


