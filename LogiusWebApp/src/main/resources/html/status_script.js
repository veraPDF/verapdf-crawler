var URL = "../crawl-job/";

function checkStatus() {
    var jobId = location.search.split("id=")[1];
    $("#email_link").attr("href","email?jobId=" + jobId)
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

            valid = result.pdfStatistics.numberOfValidPDFs;
            total = result.pdfStatistics.numberOfInvalidPDFs +
            result.pdfStatistics.numberOfValidPDFs;
            $("#valid").text(valid);
            $("#total").text(total);
            $("#odftotal").text(result.numberOfODFDocuments);
            $("#office_total").text(result.numberOfOfficeDocuments);
            if(status.substring(0, 8) != "Finished")
                setTimeout(checkStatus, 1000);
            else {
                $("#email_link").text("");
                $("#ods_report").text("Report in ODT format");
                $("#ods_report").attr("href", URL + "ods_report/" + jobId);
                $("#html_report").text("Report in HTML format");
                $("#html_report").attr("href", URL + "html_report/" + jobId);
            }
	    },
	    error: function(result) {
            $("#stats").html("<font color=\"red\">Error on getting job info.</font>")
            $("#odfs").html("");
            $("#office").html("");
	    }
	});
}
window.onload = checkStatus;