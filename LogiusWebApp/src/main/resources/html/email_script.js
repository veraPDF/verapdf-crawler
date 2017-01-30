var jobId = location.search.split("jobId=")[1];

$(document).ready(function() {
    $("#job_link").attr("href","../jobinfo?id=" + jobId)
    $("input:button").click(main);
});

function main() {
    if(document.getElementById("input").value) {
        regexp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(!regexp.test(document.getElementById("input").value)) {
            $("#error").text("Invalid email format");
            return;
        }
    }
    var postData = "{\"job\":\"" + jobId + "\"" +
    ", \"emailAddress\":\"" + document.getElementById("input").value + "\"}";
    $.ajax({url: "../crawl-job/email",
        type:"POST",
        data: postData,
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
            $("#error").text("");
            document.getElementById("input").value="Submited successfully!";
        },
        error: function(result) {
        $("#error").text("Email update failed");
        }
    });
}