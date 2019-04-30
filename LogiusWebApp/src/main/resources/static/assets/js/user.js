String.prototype.format = function () {
    var i = 0, args = arguments;
    return this.replace(/{}/g, function () {
        return typeof args[i] != 'undefined' ? args[i++] : '';
    });
};

$(document).ready(function () {
    function loadUserInfo() {

        var div = "#user-menu";
        console.log(124124)
        console.log(localStorage['token'])
        if (!localStorage.getItem('token')) {
            $("<li><a href=\"/sign-in.html\">Sign in</a></li>" +
                "<li><a href=\"/sign-up.html\">Sign Up</a></li>" +
                "<li><a href=\"/password-reset-email.html\">Password reset</a></li>"
            ).appendTo(div);
            console.log("user not authentificated");
        } else {
            $.ajax({
                url: "/api/user/me",
                type: "GET",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('token'));
                },
                success: function (accountInfo) {
                    localStorage['email'] = accountInfo.email;
                    $("<li>{}</li>".format(accountInfo.email)).appendTo(div);
                    if (accountInfo.role === 'ADMIN') {
                        $("<li><a href=\"/admin-dashboard.html\">Admin dashboard</a></li>").appendTo(div);
                    }
                    $("<li><a id=\"update-password\" href=\"/update-password.html\">Update password</a></li>").appendTo(div);
                    $("<li><a id=\"logout\" href=\".\">Logout</a></li>").appendTo(div);
                    $("#logout").click(function (e) {
                        if (localStorage['token']) {
                            localStorage.removeItem('token');
                        }
                    });
                    if (window.location.pathname === '/' || window.location.pathname === '/index.html') {
                        $("#bing-crawl-service-elem").removeAttr('style');
                        $("#validation-required-elem").removeAttr('style');
                    }
                    if (window.location.pathname === '/domains.html') {
                        $("#job-list-checkbox-p").removeAttr('style');
                    }

                },
                error: function (error) {
                    localStorage.removeItem('token');
                    localStorage.removeItem('email');
                    $(location).attr('href', '/index.html')
                }
            });
        }
    }

    loadUserInfo();
});
