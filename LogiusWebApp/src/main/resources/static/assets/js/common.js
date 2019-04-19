//todo refactor?
function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
}

function verifyPassword(password) {
    var err_message = [];
    if (password.length < 5) {
        err_message.push('must at least 6 characters long')
    }
    if (password.search(/\d/) === -1) {
        err_message.push('include ar least one number')
    }
    if (password.search(/[a-zA-Z]/) === -1) {
        err_message.push('include both lower and upper case characters')
    }
    if (password.search(/[?=.*[\]!@#$%^&()\-_+{};:,<>]/) === -1) {
        err_message.push('include ar least one specific symbol')
    }

    return err_message.length === 0 ? '' : 'your password need to: ' + err_message.join(', ');
}

function verifyEmail(value) {
    var re = /\S+@\S+\.\S+/;
    return !re.test(value);
}


function getUrlParam(param) {
    var urlParams = new URLSearchParams(window.location.search);
    console.log(urlParams.get(param));
    return urlParams.get(param);
}

function normalizeURL(url) {
    return url.replace(':', '%3A');
}

function createTooltip(id, errorMessage) {
    return $(id).tooltip({
        trigger: 'manual',
        placement: 'right',
        template: '<div class="tooltip error plasement right" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorMessage;
        }
    });
}