var URL = "/crawl-job/";

$(document).ready(function() {
    var today = new Date();
    var dd = today.getDate();
    var mm = today.getMonth()+1;
    var yyyy = today.getFullYear();

    if(dd<10) {
        dd='0'+dd
    }

    if(mm<10) {
        mm='0'+mm
    }

    today = dd+'-'+mm+'-'+yyyy;
    $("#date_input").val(today);
    $("input:button").click(main);
});

window.onload = loadAllJobs;

function main() {
    $("#report_link").text("");
    $("#number_of_crawled_urls").text("");
    $("#crawl_url").text("");
    var regexp = new RegExp("\\d\\d-\\d\\d-\\d\\d\\d\\d");
    if(!regexp.test(document.getElementById("date_input").value)) {
        reportError("Invalid date format");
        return;
    }
    regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
    if(!regexp.test(document.getElementById("urlinput").value)) {
        reportError("Invalid url format");
        return;
    }
    if(document.getElementById("email_input").value) {
        regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
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
