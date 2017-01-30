var jobId = location.search.split("jobId=")[1];

$(document).ready(function() {
    $("#job_link").attr("href","../jobinfo?id=" + jobId)
    $("input:button").click(main);
});

function main() {
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