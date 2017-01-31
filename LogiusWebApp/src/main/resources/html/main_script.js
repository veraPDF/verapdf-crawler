var URL = "/crawl-job/";

$(document).ready(function() {
    $("input:button").click(main);
});

window.onload = loadAllJobs;

function main() {
    $("#report_link").text("");
    $("#number_of_crawled_urls").text("");
    $("#crawl_url").text("");
    if(document.getElementById("date_input").value) {
        var regexp = new RegExp("\\d\\d-\\d\\d-\\d\\d\\d\\d");
        if(!regexp.test(document.getElementById("date_input").value)) {
            reportError("Invalid date format");
            return;
        }
    }
    if(document.getElementById("email_input").value) {
        var regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(!regexp.test(document.getElementById("email_input").value)) {
            reportError("Invalid email format");
            return;
        }
    }

    var crawlUrlList = new Array(document.getElementById("urlinput").value);
    var status;
    var postData = "{\"domain\":\"" + document.getElementById("urlinput").value +
    "\", \"date\":\"" + document.getElementById("date_input").value + "\"" +
    ", \"reportEmail\":\"" + document.getElementById("email_input").value + "\"" +
    ", \"forceStart\":\"" + $("#force_start").is(":checked") + "\"}";
    $.ajax({url: URL,
        type:"POST",
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
           loadAllJobs()
        },
        error: function(result) {
                reportError("Error on job creation");
        }
    });
}

function appendCrawlJob(id, url) {
    var ul = document.getElementById("crawl_url_list");
    var li = document.createElement("li");
    var link = document.createElement("a");
    link.setAttribute("href","jobinfo?id=" + id)
    link.innerHTML = "Job on " + url;

    li.appendChild(link);
    ul.appendChild(li);
}

function loadAllJobs() {
    $.ajax({url: URL + "list",
            type:"GET",
            success: function(result){
                $("#crawl_url_list").empty();
                for(var key in result) {
                    if(result.hasOwnProperty(key)) {
                        appendCrawlJob(key, result[key]);
                    }
                }
            },
            error: function(result) {
                reportError("Error on job loading");
            }
    });

    var crawlUrl = location.search.split("text=")[1];
    if(crawlUrl && 0 === crawlUrl.length) {
        document.getElementById("urlinput").value = crawlUrl;
        main();
    }
}

function reportError(text) {
    var ul = document.getElementById("crawl_url_list");
    var li = document.createElement("li");
    var link = document.createElement("p");
    link.innerHTML = "<font color=\"red\">" + text + ".</font>"
    li.appendChild(link);
    ul.appendChild(li);
}

function keyListener(e) {
    if(e.keyCode == 13) {
        main();
    }
}
