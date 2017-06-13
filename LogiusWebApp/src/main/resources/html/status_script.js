var URL = "../api/";

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
            $("#start_time").text("Job started on " + result.startTime);
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
            if(status.substring(0, 8) != "finished" || status.substring(0, 8) != "aborted")
                setTimeout(checkStatus, 1000);
            else {
                $("#finish_time").text("Job finished on " + result.finishTime);
                $("#email_link").hide();
                $("#ods_report").text("Download report in ODS format");
                $("#ods_report").attr("href", URL + "report/ods_report/" + jobId);
                $("#ods_report").show();
                $.ajax({ url: URL + "report/html_report/" + jobId,
                        type:"GET",
                        async:false,
                        success:function(result) {
                            $("#ods_report").addClass('hidden-element');
                            $('#stats').addClass('hidden-element');
                            $('#office').addClass('hidden-element');
                            $('#odfs').addClass('hidden-element');
                            $('#number_of_crawled_urls').addClass('hidden-element');

                            $('#html-report').removeClass('hidden-element')
                            $("#html-report").html(result);
                        },
                        error: function(result) {
                            $("#stats").html("<font color=\"red\">Failed to get HTML job report.</font>");
                        }});
            }
	    },
	    error: function(result) {
            $("#stats").html("<font color=\"red\">Error on getting job info.</font>");
            $("#odfs").html("");
            $("#office").html("");
	    }
	});
}
window.onload = checkStatus;