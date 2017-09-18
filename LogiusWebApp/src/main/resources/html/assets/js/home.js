var URL = "/api/crawl-requests";

$(document).ready(function () {
    document.getElementById("date_input").value = "01-01-2015";
    $("input:button").click(main);

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

    if (document.getElementById("email_input").value) {
        var regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if (!regexp.test(document.getElementById("email_input").value)) {
            return;
        }
    }

    var crawlUrlList = document.getElementById("urlinput").value.split(', ');
    var urlRegExp = /^.+\..+$/

    for (var i = 0; i < crawlUrlList.length; i++) {
        if (!urlRegExp.test(crawlUrlList[i])) {
            return;
        }
    }
    var postData = {};
    postData.domains = crawlUrlList;
    postData.emailAddress = document.getElementById("email_input").value;
    postData.crawlSinceTime = document.getElementById("date_input").value;

    $.ajax({
        url: URL,
        type: "POST",
        data: JSON.stringify(postData),
        async: false,
        headers: {
            "content-type": "application/json"
        },
        success: function (result) { },
        error: function (result) { }
    });
}

function keyListener(e) {
    if (e.keyCode == 13) {
        main();
    }
}
