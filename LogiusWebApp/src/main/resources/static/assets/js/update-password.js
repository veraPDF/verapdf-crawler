$(document).ready(function () {
    var oldPasswordInput = createTooltip('#password_old', 'Incorrect password');
    var newPasswordInput = createTooltip('#password', 'Incorrect password');
    var repeatPasswordInput = createTooltip('#password_repeat', 'Please enter the same password as above');
    var requestSubmit = createTooltip('#updatePasswordSubmit', '');

    function hasErrors(json) {
        var verifyOld = verifyPassword(json[1]['value']);
        var verifyNew = verifyPassword(json[1]['value']);
        var isOldPasswordIncorrect = verifyOld !== '';
        var isNewPasswordIncorrect = verifyNew !== '';
        var isRepeatPasswordIncorrect = json[1]['value'] !== json[2]['value'];
        isOldPasswordIncorrect ? oldPasswordInput.tooltip('show') : oldPasswordInput.tooltip('hide');
        isNewPasswordIncorrect ? newPasswordInput.tooltip('show') : newPasswordInput.tooltip('hide');
        isRepeatPasswordIncorrect ? repeatPasswordInput.tooltip('show') : repeatPasswordInput.tooltip('hide');
        return isOldPasswordIncorrect || isNewPasswordIncorrect || isRepeatPasswordIncorrect;
    }


    $("#update-password-form").submit(function (e) {
        e.preventDefault();
    });

    $("#updatePasswordSubmit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#update-password-form').serializeArray()));
        requestSubmit.tooltip('hide');
        if (hasErrors(json)) {
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
                var message = '';
                if (JSON.parse(error.responseText).errors) {
                    message = JSON.parse(error.responseText).errors.join('\n');
                } else {
                    message = JSON.parse(error.responseText).message;
                }
                requestSubmit.attr('data-original-title', message).tooltip('show')
            }
        });
    });
});