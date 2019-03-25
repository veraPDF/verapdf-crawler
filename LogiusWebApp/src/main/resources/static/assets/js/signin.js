$(document).ready(function () {
    $("#login-form").submit(function (e) {
        e.preventDefault();
    });

    $("#loginSubmit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#login-form').serializeArray()));
        $.ajax({
            beforeSend: function (xhr) {
                xhr.setRequestHeader ("Authorization", "Basic " + btoa(json[0]['value'] + ":" + json[1]['value']));
            },
            type: "GET",
            url: "/api/auth/token",
            headers: {"Content-type": "application/json"},
            success: function (response) {
                localStorage.setItem('token', response);
                $(location).attr('href', '/')
            },
            error: function (error) {
                $("#login-err-message").text(JSON.parse(error.responseText).errors.join('\n'));
            }
        });
    });
});