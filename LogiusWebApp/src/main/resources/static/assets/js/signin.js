$(document).ready(function () {
    var emailInput = createTooltip('#email', 'Incorrect email or user with that email not exists');
    var passwordInput = createTooltip('#password', 'Incorrect password');
    var requestSubmit = createTooltip('#loginSubmit', '');

    function hasErrors(json) {
        var verify = verifyPassword(json[1]['value']);
        var isEmailIncorrect = verifyEmail(json[0]['value']);
        var isPasswordIncorrect = verify !== '';
        isEmailIncorrect ? emailInput.tooltip('show') : emailInput.tooltip('hide');
        isPasswordIncorrect ? passwordInput.tooltip('show') : passwordInput.tooltip('hide');
        return isEmailIncorrect || isPasswordIncorrect;
    }

    $("#login-form").submit(function (e) {
        e.preventDefault();
    });

    $("#loginSubmit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#login-form').serializeArray()));
        requestSubmit.tooltip('hide');
        if (hasErrors(json)){
            return
        }
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
                requestSubmit.attr('data-original-title',  JSON.parse(error.responseText).errors.join('\n')).tooltip('show')
            }
        });
    });
});