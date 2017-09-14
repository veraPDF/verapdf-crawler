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
            // reportError("Invalid email format");
            return;
        }
    }

    var crawlUrlList = new Array(document.getElementById("urlinput").value);
    var postData = {};
    postData.domains = [];
    postData.domains.push(document.getElementById("urlinput").value);
    postData.emailAddress = document.getElementById("date_input").value;
    postData.crawlSinceTime = document.getElementById("email_input").value;

    $.ajax({
        url: URL,
        type: "POST",
        data: JSON.stringify(postData),
        async: false,
        headers: {
            "content-type": "application/json"
        },
        success: function (result) { },
        error: function (result) {
            // reportError("Error on job creation");
        }
    });
}

// function appendCrawlJob(id, url) {
//     var ul = document.getElementById("crawl_url_list");
//     var li = document.createElement("li");
//     var link = document.createElement("a");
//     link.setAttribute("href","jobinfo?id=" + id)
//     link.innerHTML = "Job on " + url;

//     li.appendChild(link);
//     ul.appendChild(li);
// }

// function loadAllJobs() {
//     $.ajax({url: URL + "info/list",
//             type:"GET",
//             success: function(result){
//                 $("#crawl_url_list").empty();
//                 result.forEach(function(item, i, arr){
//                     appendCrawlJob(item.id, item.crawlURL + " - " + item.status)
//                 });
//                 setTimeout(loadAllJobs, 3000);
//             },
//             error: function(result) {
//                 reportError("Error on job loading");
//             }
//     });

//     var crawlUrl = location.search.split("text=")[1];
//     if(crawlUrl && 0 === crawlUrl.length) {
//         document.getElementById("urlinput").value = crawlUrl;
//         main();
//     }
// }

// function reportError(text) {
//     var ul = document.getElementById("crawl_url_list");
//     var li = document.createElement("li");
//     var link = document.createElement("p");
//     link.innerHTML = "<font color=\"red\">* " + text + ".</font>"
//     li.appendChild(link);
//     ul.innerHTML = '';
//     ul.appendChild(li);
// }

function keyListener(e) {
    if (e.keyCode == 13) {
        main();
    }
}
