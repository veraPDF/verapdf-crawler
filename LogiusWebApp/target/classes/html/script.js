var URL = "/crawl-job/";

function checkStatus(jobId) {
    $.ajax({url: URL + jobId,
            type:"GET",
            async:false,
	    success: function(result){
            status = result.status;
            $("#div1").text("Job is " + status);
            if(status.substring(0, 8) == "Finished") {
                $("#div2").text(result.reportUrl);
                $("#div2").html("<a href=\"" + result.reportUrl + "\">Crawl log</a>");
            }
            else {
                setTimeout(checkStatus, 1000, jobId);
            }
	    }
	});
}

function main() {
    $("#div2").text("");
    var jobId;
    var status;
    var postData = "{\"domain\":\"" + document.getElementById("urlinput").value + "\"}";
    $.ajax({url: URL,
        type:"POST",
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
            jobId = result.id;
            status = result.status;
            $("#div1").text("Job is " + status);
            checkStatus(jobId);
        }
    });
}

$(document).ready(function() {
    $("input:button").click(main);
});

/*function handlEvent(e) {
    if(e.keyCode === 13){
        main();
    }
}*/