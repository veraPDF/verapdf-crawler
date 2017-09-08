$(function () {
    // Global chart settings
    Chart.defaults.global.animation.duration = 0;
    Chart.defaults.global.maintainAspectRatio = false;
    Chart.defaults.global.legend.display = false;
    Chart.defaults.global.title.fontColor = 'white';
    Chart.defaults.global.title.fontSize = 14;
    Chart.scaleService.updateScaleDefaults('linear', {
        color: 'white',
        ticks: {
            fontColor: 'white'
        },
        gridLines: {
            color: 'rgba(255, 255, 255, 0.1)',
            zeroLineColor: 'rgba(255, 255, 255, 0.25)'
        }
    });
    Chart.scaleService.updateScaleDefaults('category', {
        color: 'white',
        ticks: {
            fontColor: 'white'
        },
        gridLines: {
            color: 'rgba(255, 255, 255, 0.1)',
            zeroLineColor: 'rgba(255, 255, 255, 0.25)'
        }
    });

    // Enable tooltips
    $('[data-toggle="tooltip"]').tooltip();


    // Retrieve domain information
    // var domains = {
    //     'www.duallab.com': {
    //         crawlURL: 'www.duallab.com',
    //         startTime: '25 Aug 2017, 16:36',
    //         finishTime: '25 Aug 2017, 17:15',
    //         status: 'finished',
    //         mailsList: ['example1@report.com', 'example2@report.com']
    //     },
    //     'www.vananaarbeter.nl': {
    //         crawlURL: 'www.vananaarbeter.nl',
    //         startTime: '25 Aug 2017, 16:36',
    //         status: 'running',
    //         mailsList: ['example1@report.com', 'example2@report.com']
    //     },
    //     'secure5.svb.nl': {
    //         crawlURL: 'secure5.svb.nl',
    //         startTime: '25 Aug 2017, 16:36',
    //         status: 'paused',
    //         mailsList: ['example1@report.com', 'example2@report.com']
    //     }
    // };
    // var currentDomainName = getUrlParameter("domain");
    var currentDomain = [];//domains[currentDomainName];

    function domainInfoLoaded(job) {
        currentDomain = job;
        currentDomain.isComplete = currentDomain.status === 'finished' || currentDomain.status === 'failed';

        $('.main').addClass('status-' + currentDomain.status, { children: true });

        $('.domain-name span').text(currentDomain.crawlURL);

        $('.job-date').text(currentDomain.isComplete ? 'Tested on ' + currentDomain.startTime + ' - ' + currentDomain.finishTime : 'Test started on ' + currentDomain.startTime);

        $('.status-text').text(currentDomain.status);

        if (!currentDomain.isComplete) {
            $('.job-mails').addClass('editable');
        }

        $('.job-mails .label').text(currentDomain.isComplete ? 'Report sent to:' : 'Send report to:');

        // $('span.job-mails-list').text(currentDomain.mailsList.join(', '));
        // $('textarea.job-mails-list').val(currentDomain.mailsList.join(', '));
    }

    var errorMessage = '';
    var emailTextArea = $('.job-mails textarea.job-mails-list').tooltip({
        trigger: 'manual',
        template: '<div class="tooltip error" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
        title: function () {
            return errorMessage;
        }
    }).on('focusout', validateEmails);

    function validateEmails() {
        var content = emailTextArea.val();
        var emails = content.split(/\s*,\s*/);
        var invalidEmails = [];
        $.each(emails, function (index, email) {
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                invalidEmails.push(email);
            }
        });
        if (invalidEmails.length > 0) {
            errorMessage = 'The following emails are invalid: ' + invalidEmails.join(', ');
            emailTextArea.addClass('error').tooltip('show');
            return { valid: false };
        } else {
            emailTextArea.removeClass('error').tooltip('hide');
        }
        return { valid: true, emails: emails };
    }

    $('.job-mails .edit').on('click', function () {
        $('.job-mails').addClass('edit-mode');
    });

    $('.job-mails .done').on('click', function () {
        var validationResult = validateEmails();
        if (validationResult.valid) {
            currentDomain.mailsList = validationResult.emails;
            $('span.job-mails-list').text(currentDomain.mailsList.join(', '));
            $('.job-mails').removeClass('edit-mode');
        }
    });

    $('.job-mails .cancel').on('click', function () {
        emailTextArea.val(currentDomain.mailsList.join(', ')).removeClass('error').tooltip('hide');
        $('.job-mails').removeClass('edit-mode');
    });

    $.ajax({
        url: "/crawl-jobs/" + getUrlParameter("domain"),
        type: "GET",
        success: function (result) {
            domainInfoLoaded(result);

        },
        error: function (result) {
            // reportError("Error on job loading");
        }
    });

    $.ajax({
        url: "/crawl-jobs/" + getUrlParameter("domain") + "/requests",
        type: "GET",
        success: function (result) {
            // domainInfoLoaded(result);

            $('span.job-mails-list').text(result.join(', '));
            $('textarea.job-mails-list').val(result.join(', '));

        },
        error: function (result) {
            // reportError("Error on job loading");
        }
    });

    $("#action-resume").on('click', function () {        
        $.ajax({
            url: "/crawl-jobs/" + getUrlParameter("domain") + "/requests",
            type: "PUT",
            data: "{\"domain\":\" fikytfkutf \"}",
            success: function (result) {
                // domainInfoLoaded(result);
    
    
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });


    $("#action-pause").on('click', function () {
        $.ajax({
            url: "/crawl-jobs/" + getUrlParameter("domain") + "/requests",
            type: "PUT",
            // async:false,
            data: "{\"domain\":\" fikytfkutf \"}",
            headers: {"Content-type":"application/json"},
            success: function (result) {
                // domainInfoLoaded(result);    
    
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });

    $("#action-restart").on('click', function () {

    });

    // Summary tab components

    var summaryDatePicker = new Pikaday({
        field: document.getElementById('summary-date-input'),
        firstDay: 1,
        minDate: new Date(2017, 7, 4),
        maxDate: new Date(2020, 12, 31),
        yearRange: [2000, 2020],
        showTime: false,
        format: 'DD-MM-YYYY'
    });

    var summaryChartContext = document.getElementById("summary-chart").getContext('2d');
    var summaryChart = new Chart(summaryChartContext, {
        type: 'pie',
        data: {
            labels: ["To improve", "Compliant"],
            datasets: [{
                data: [2437, 1445],
                backgroundColor: [
                    '#fd5858',
                    '#43c46f'
                ],
                borderWidth: 0
            }]
        }
    });

    // Documents tab components

    var documentsDatePicker = new Pikaday({
        field: document.getElementById('documents-date-input'),
        firstDay: 1,
        minDate: new Date(2017, 7, 4),
        maxDate: new Date(2020, 12, 31),
        yearRange: [2000, 2020],
        showTime: false,
        format: 'DD-MM-YYYY'
    });

    var flavorsChartContext = document.getElementById("flavors-chart").getContext('2d');
    var flavorsChart = new Chart(flavorsChartContext, {
        type: 'bar',
        data: {
            labels: ['PDF/A-1a', 'PDF/A-1b', 'PDF/A-2a', 'PDF/A-2b', 'PDF/A-2u', 'PDF/A-3a', 'PDF/A-3b', 'PDF/A-3u', 'Other PDF'],
            datasets: [{
                data: [523, 26, 780, 313, 231, 23, 0, 0, 1278],
                backgroundColor: ['white', 'white', 'white', 'white', 'white', 'white', 'white', 'white', 'white', 'white']
            }]
        },
        options: {
            title: {
                display: true,
                text: 'PDF/A flavors'
            }
        }
    });

    var versionsChartContext = document.getElementById("versions-chart").getContext('2d');
    var versionsChart = new Chart(versionsChartContext, {
        type: 'bar',
        data: {
            labels: ['PDF 1.0', 'PDF 1.1', 'PDF 1.2', 'PDF 1.3', 'PDF 1.4', 'PDF 1.5', 'PDF 1.6', 'PDF 1.7', 'PDF 2.0'],
            datasets: [{
                data: [0, 26, 324, 780, 34, 123, 412, 753, 0],
                backgroundColor: ['white', 'white', 'white', 'white', 'white', 'white', 'white', 'white', 'white', 'white']
            }]
        },
        options: {
            title: {
                display: true,
                text: 'PDF versions'
            }
        }
    });

    var producersChartContext = document.getElementById("producers-chart").getContext('2d');
    var producersChart = new Chart(producersChartContext, {
        type: 'bar',
        data: {
            labels: ['Skia/PDF', 'OpenOffice', 'iText', 'Acrobat Distiller', 'Microsoft Word', 'Other'],
            datasets: [{
                data: [535, 345, 265, 145, 123, 1234],
                backgroundColor: ['white', 'white', 'white', 'white', 'white', 'white']
            }]
        },
        options: {
            title: {
                display: true,
                text: 'PDF producers'
            }
        }
    });

    // Errors tab components

    var engine = new Bloodhound({
        local: ['Skia/PDF', 'OpenOffice', 'iText', 'Acrobat Distiller', 'Microsoft Word'],
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        datumTokenizer: Bloodhound.tokenizers.whitespace
    });
    $('#errors-producer-input').typeahead({
        minLength: 1,
        highlight: true
    },
        {
            name: 'producers',
            source: engine
        });

    var errorsDatePicker = new Pikaday({
        field: document.getElementById('errors-date-input'),
        firstDay: 1,
        minDate: new Date(2017, 7, 4),
        maxDate: new Date(2020, 12, 31),
        yearRange: [2000, 2020],
        showTime: false,
        format: 'DD-MM-YYYY'
    });

    var errorsChartContext = document.getElementById("errors-chart").getContext('2d');
    var errorsChart = new Chart(errorsChartContext, {
        type: 'pie',
        data: {
            labels: ["Error 1: description", "Error 2: description", "Error 3: description", "Error 4: description", "Error 5: description", "Error 6: description", "Error 7: description", "Error 8: description", "Error 9: description", "Error 10: description", "Other"],
            datasets: [{
                data: [978, 750, 564, 550, 300, 50, 18, 5, 1, 1, 564],
                backgroundColor: [
                    '#fd5858',
                    '#fd9651',
                    '#fdeb72',
                    '#cdfd92',
                    '#9cfdc2',
                    '#a7fafd',
                    '#94a8fd',
                    '#ca94fd',
                    '#fd89e9',
                    '#ff9ca0',
                    '#ffffff'
                ],
                borderWidth: 0
            }]
        }
    });
});