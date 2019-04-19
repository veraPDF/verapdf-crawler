function getActivateToken() {
    var urlParams = new URLSearchParams(window.location.search);
    console.log(urlParams.get('token'));
    return urlParams.get('token');
}

$(document).ready(function () {
    var newPasswordInput = createTooltip('#password', 'Incorrect password');
    var repeatPasswordInput = createTooltip('#password_repeat', 'Please enter the same password as above');
    var requestSubmit = createTooltip('#password-reset-submit', '');

    function hasErrors(json) {
        var verifyOld = verifyPassword(json[1]['value']);
        var isOldPasswordIncorrect = verifyOld !== '';
        var isRepeatPasswordIncorrect = json[0]['value'] !== json[1]['value'];
        isOldPasswordIncorrect ? newPasswordInput.tooltip('show') : newPasswordInput.tooltip('hide');
        isRepeatPasswordIncorrect ? repeatPasswordInput.tooltip('show') : repeatPasswordInput.tooltip('hide');
        return isOldPasswordIncorrect || isRepeatPasswordIncorrect;
    }

    $("#password-reset-form").submit(function (e) {
        e.preventDefault();
    });

    $("#password-reset-submit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#password-reset-form').serializeArray()));
        requestSubmit.tooltip('hide');
        if (hasErrors(json)){
            return;
        }
        var j = {
            "newPassword": json[1]['value']
        };

        $.ajax({
            type: "POST",
            url: "/api/user/password-reset-confirm",
            data: JSON.stringify(j),
            headers: {"Content-type": "application/json", "Authorization": "Bearer " + getActivateToken()},
            success: function (response) {
                $(location).attr('href', '/sign-in.html')
            },
            error: function (error) {
                requestSubmit.attr('data-original-title',  JSON.parse(error.responseText).errors.join('\n')).tooltip('show')
            }
        });
    });

    if (!getActivateToken()){
        $(location).attr('href', '/index.html')
    }
});