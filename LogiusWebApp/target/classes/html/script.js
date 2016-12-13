var URL = "/crawl-job/";
//var currentJobId;

$(document).ready(function() {
    $("input:button").click(main);
});

function main() {
    $("#report_link").text("");
    $("#number_of_crawled_urls").text("");
    $("#crawl_url").text("");
    var crawlUrlList = new Array(document.getElementById("urlinput").value);
    var status;
    var postData = "{\"domain\":\"" + document.getElementById("urlinput").value + "\"}";
    $.ajax({url: URL,
        type:"POST",
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
            appendCrawlJob(result.id, result.url);

            /*currentJobId = result.id;
            status = result.status;
            $("#status").text("Job is " + status);
            checkStatus();*/
        }
    });
}

/*function setCurrentJobId(id) {
    currentJobId = id;
    checkStatus();
}

function checkStatus() {
    jobId = currentJobId;
    $.ajax({url: URL + jobId,
            type:"GET",
            async:false,
	    success: function(result){
            status = result.status;
            number = result.numberOfCrawledUrls;
            crawlUrl = result.url;
            $("#status").text("Job is " + status);
            $("#crawl_url").text("Crawling url " + crawlUrl);
            if(undefined != number)
                $("#number_of_crawled_urls").text(number + " urls crawled.");
            if(status.substring(0, 8) == "Finished") {
                $("#report_link").text(result.reportUrl);
                $("#report_link").html("<a href=\"" + result.reportUrl + "\">Crawl log</a>");
            }
            else {
                $("#report_link").text("");
                setTimeout(checkStatus, 1000);
            }
	    }
	});
}*/

function appendCrawlJob(id, url) {
    var ul = document.getElementById("crawl_url_list");
    var li = document.createElement("li");
    var link = document.createElement("a");
    link.setAttribute("href","jobinfo?id=" + id)
    link.innerHTML = "Job on " + url;

    li.appendChild(link);
    ul.appendChild(li);
}
