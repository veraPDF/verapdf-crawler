function getActivateToken() {
    var urlParams = new URLSearchParams(window.location.search);
    console.log(urlParams.get('token'));
    return urlParams.get('token');
}

$(document).ready(function () {
    var emailInput = createTooltip('#email', 'Incorrect email or user with that email not exists');
    var requestSubmit = createTooltip('#password-reset-submit', '');

    function hasErrors(json) {
        var re = /\S+@\S+\.\S+/;
        var isEmailIncorrect = !re.test(json[0]['value']);
        isEmailIncorrect ? emailInput.tooltip('show') : emailInput.tooltip('hide');
        return isEmailIncorrect;
    }

    $("#password-reset-form").submit(function (e) {
        e.preventDefault();
    });

    $("#password-reset-submit").click(function (e) {
        var json = JSON.parse(JSON.stringify(jQuery('#password-reset-form').serializeArray()));

        if (hasErrors(json)){
            return;
        }
        $.ajax({
            type: "POST",
            url: "/api/user/email-resend?email=" + json[0]['value'] ,
            headers: {"Content-type": "application/json"},
            success: function (response) {
                $(location).attr('href', '/index.html')
            },
            error: function (error) {
                requestSubmit.attr('data-original-title',  JSON.parse(error.responseText).errors.join('\n')).tooltip('show')
            }
        });
    });

    if (localStorage.getItem("token")){
        $(location).attr('href', '/index.html')
    }
});