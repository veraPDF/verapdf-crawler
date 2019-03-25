$(document).ready(function () {
    $("#update-password-form").submit(function (e) {
        e.preventDefault();
    });

    $("#updatePasswordSubmit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#update-password-form').serializeArray()));
        console.log(json)
        if (json[1]['value'] !== json[2]['value']) {
            $("#password-update-err-message").text('Please enter the same password as above');
            return;
        }
        var j = {
            "oldPassword": json[0]['value'],
            "newPassword": json[1]['value']
        };
        $.ajax({
            type: "PUT",
            url: "/api/user/password",
            data: JSON.stringify(j),
            headers: {
                "Content-type": "application/json",
                'Authorization': 'Bearer ' + localStorage['token']
            },
            success: function (response) {
                localStorage.removeItem('token');
                $(location).attr('href', '/sign-in.html');
            },
            error: function (error) {
                if (JSON.parse(error.responseText).errors){
                    $("#password-update-err-message").text(JSON.parse(error.responseText).errors.join('\n'));
                } else {
                    $("#password-update-err-message").text(JSON.parse(error.responseText).message);
                }
            }
        });
    });
});