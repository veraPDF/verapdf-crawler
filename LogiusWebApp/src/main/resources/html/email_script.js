$(document).ready(function() {
    $("input:button").click(main);
});

function main() {
    var postData = "{\"emailAddress\":\"" + document.getElementById("input").value + "\"}";
    $.ajax({url: "../crawl-job/target_email",
        type:"POST",
        async:false,
        headers: {"Content-type":"application/json"}, data:postData,
        success: function(result){
            document.getElementById("input").value="Submited successfully!";
        }
    });
}