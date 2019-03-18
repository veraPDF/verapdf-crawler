$(document).ready(function () {
    $("#sign-up-form").submit(function (e) {
        e.preventDefault();
    });

    $("#signupSubmit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#sign-up-form').serializeArray()));
        if (json[1]['value'] !== json[2]['value']){
            $("#signup-err-message").text('Please enter the same password as above');
            return;
        }
        var j = {
            "email": json[0]['value'],
            "password": json[1]['value']
        };
        $.ajax({
            type: "POST",
            url: "/api/auth/signup",
            data: JSON.stringify(j),
            headers: {"Content-type": "application/json"},
            success: function (response) {
                $(location).attr('href', '/sign-in.html')
            },
            error: function (error) {
                $("#signup-err-message").text(JSON.parse(error.responseText).errors.join('\n'));
            }
        });
    });
});