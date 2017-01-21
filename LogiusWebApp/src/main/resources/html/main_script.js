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
    var crawlUrlList = new Array(document.getElementById("urlinput").value);
    var status;
    var postData = "{\"domain\":\"" + document.getElementById("urlinput").value +
    "\", \"date\":\"" + document.getElementById("date_input").value + "\"}";
    $.ajax({url: URL,
        type:"POST",
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
           loadAllJobs()
        },
        error: function(result) {
            var ul = document.getElementById("crawl_url_list");
            var li = document.createElement("li");
            var link = document.createElement("p");
            link.innerHTML = "<font color=\"red\">Error on job creation.</font>"
            li.appendChild(link);
            ul.appendChild(li);
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
                var ul = document.getElementById("crawl_url_list");
                var li = document.createElement("li");
                var link = document.createElement("p");
                link.innerHTML = "<font color=\"red\">Error on job loading.</font>"
                li.appendChild(link);
                ul.appendChild(li);
            }
    });

    var crawlUrl = location.search.split("text=")[1];
    if(crawlUrl && 0 === crawlUrl.length) {
        document.getElementById("urlinput").value = crawlUrl;
        main();
    }
}

function keyListener(e) {
    if(e.keyCode == 13) {
        main();
    }
}
