$(document).ready(function () {
    var emailInput = createTooltip('#email', 'Incorrect email');
    var passwordInput = createTooltip('#password', 'Incorrect password');
    var repeatPassword = createTooltip("#password_repeat", 'Please enter the same password as above');
    var requestSubmit = createTooltip('#signupSubmit', '');

    function hasErrors(json) {
        var verify = verifyPassword(json[1]['value']);
        var isEmailIncorrect = verifyEmail(json[0]['value']);
        var isPasswordIncorrect = verify !== '';
        var isRepeatPasswordIncorrect = json[1]['value'] !== json[2]['value'];
        isEmailIncorrect ? emailInput.tooltip('show') : emailInput.tooltip('hide');
        isPasswordIncorrect ? passwordInput.tooltip('show') : passwordInput.tooltip('hide');
        isRepeatPasswordIncorrect ? repeatPassword.tooltip('show') : repeatPassword.tooltip('hide');
        return isEmailIncorrect || isPasswordIncorrect || isRepeatPasswordIncorrect;
    }


    $("#sign-up-form").submit(function (e) {
        e.preventDefault();
    });

    $("#signupSubmit").click(function (e) {
        requestSubmit.tooltip('hide')
        var json = JSON.parse(JSON.stringify(jQuery('#sign-up-form').serializeArray()));
        if (hasErrors(json)) {
            return;
        }
        var j = {
            "email": json[0]['value'],
            "password": json[1]['value']
        };
        $.ajax({
            type: "POST",
            url: "/api/user/signup",
            data: JSON.stringify(j),
            headers: {"Content-type": "application/json"},
            success: function (response) {
                $(location).attr('href', '/sign-in.html')
            },
            error: function (error) {
                requestSubmit.attr('data-original-title',  JSON.parse(error.responseText).errors.join('\n')).tooltip('show')
            }
        });
    });
});