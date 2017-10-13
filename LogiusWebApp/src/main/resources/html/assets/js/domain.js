$(function () {
    var FLAVOURS = {
        '1A': {
            displayName: 'PDF/A-1A',
            dataSetIndex: 0
        },
        '1B': {
            displayName: 'PDF/A-1B',
            dataSetIndex: 1
        },
        '2A': {
            displayName: 'PDF/A-2A',
            dataSetIndex: 2
        },
        '2B': {
            displayName: 'PDF/A-2B',
            dataSetIndex: 3
        },
        '2U': {
            displayName: 'PDF/A-2U',
            dataSetIndex: 4
        },
        '3A': {
            displayName: 'PDF/A-3A',
            dataSetIndex: 5
        },
        '3B': {
            displayName: 'PDF/A-3B',
            dataSetIndex: 6
        },
        '3U': {
            displayName: 'PDF/A-3U',
            dataSetIndex: 7
        },
        'None': {
            displayName: 'None',
            dataSetIndex: 8
        }
    };
    var VERSIONS = {
        '1.0': {
            displayName: 'PDF 1.0',
            dataSetIndex: 0
        },
        '1.1': {
            displayName: 'PDF 1.1',
            dataSetIndex: 1
        },
        '1.2': {
            displayName: 'PDF 1.2',
            dataSetIndex: 2
        },
        '1.3': {
            displayName: 'PDF 1.3',
            dataSetIndex: 3
        },
        '1.4': {
            displayName: 'PDF 1.4',
            dataSetIndex: 4
        },
        '1.5': {
            displayName: 'PDF 1.5',
            dataSetIndex: 5
        },
        '1.6': {
            displayName: 'PDF 1.6',
            dataSetIndex: 6
        },
        '1.7': {
            displayName: 'PDF 1.7',
            dataSetIndex: 7
        },
        '2.0': {
            displayName: 'PDF 2.0',
            dataSetIndex: 8
        }
    };
    var ERROR_BACKGROUNDS = [
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
    ];

    // Global chart settings
    Chart.defaults.global.animation.duration = 0;
    Chart.defaults.global.maintainAspectRatio = false;
    Chart.defaults.global.legend.display = false;
    Chart.defaults.global.title.fontColor = 'white';
    Chart.defaults.global.title.fontSize = 14;
    Chart.scaleService.updateScaleDefaults('linear', {    // y-Axis
        color: 'white',
        ticks: {
            fontColor: 'white',
            beginAtZero: true,
            fixedStepSize: 1
        },
        gridLines: {
            color: 'rgba(255, 255, 255, 0.1)',
            zeroLineColor: 'rgba(255, 255, 255, 0.25)'
        }
    });
    Chart.scaleService.updateScaleDefaults('category', {   // x-Axis
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


    var currentDomain = {};
    function normalizeURL(url){
        return url.replace(':', '%3A');
    }

    function domainInfoLoaded(job) {
        currentDomain = job;
        currentDomain.isComplete = currentDomain.status === 'FINISHED' || currentDomain.status === 'FAILED';

        
        $('.main').addClass('status-' + currentDomain.status.toLowerCase(), { children: true });

        $('.domain-name span').text(currentDomain.domain);

        $('.job-date').text(currentDomain.isComplete ? 'Tested on ' + currentDomain.startTime + ' - ' + currentDomain.finishTime : 'Test started on ' + currentDomain.startTime);

        $('.status-text').text(currentDomain.status).attr('href', 'domain-status.html?domain=' + currentDomain.domain);

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

    $("#summary-date-input").on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadSummaryData();
        }
    });

    function loadSummaryData() {
        var url = "api/report/summary?domain=" + currentDomain.domain;
        var startDate = $("#summary-date-input")[0].value;
        if (startDate !== "") {
            url += "&startDate=" + startDate;
        }
        $.ajax({
            url: url,
            type: "GET",
            success: function (result) {
                $('.summary .good-documents .pdf').text(result['openDocuments']['pdf']);
                $('.summary .good-documents .office').text(result['openDocuments']['office']);
                $('.summary .bad-documents .pdf').text(result['notOpenDocuments']['pdf']);
                $('.summary .bad-documents .office').text(result['notOpenDocuments']['office']);

                var openCount = result['openDocuments']['pdf'] + result['openDocuments']['office'];
                var notOpenCount = result['notOpenDocuments']['pdf'] + result['notOpenDocuments']['office'];
                var totalCount = openCount + notOpenCount;
                var openPercent = totalCount === 0 ? 0 : Math.round(openCount * 100 / totalCount);
                var notOpenPercent = totalCount === 0 ? 0 : 100 - openPercent;

                $('.summary .good-documents .percent').text(openPercent + '%');
                $('.summary .bad-documents .percent').text(notOpenPercent + '%');

                summaryChart.data.datasets[0].data[0] = notOpenCount;
                summaryChart.data.datasets[0].data[1] = openCount;
                summaryChart.update()
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    }

    $("#documents-date-input").on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadDocumentsData();
        }
    });

    function loadDocumentsData() {
        var url = "/api/report/document-statistics?domain=" + currentDomain.domain;
        var startDate = $("#documents-date-input")[0].value;
        if (startDate !== "") {
            url += "&startDate=" + startDate;
        }
        $.ajax({
            url: url,
            type: "GET",
            success: function (result) {
                // Counts
                $('.documents .total-count').text(result['totalPdfDocumentsCount']);
                $('.documents .open-count').text(result['openPdfDocumentsCount']);
                $('.documents .not-open-count').text(result['notOpenPdfDocumentsCount']);

                // Flavours chart
                flavourChartDataset.data = [];
                $.each(FLAVOURS, function() {
                    flavourChartDataset.data.push(0);
                });

                var flavouredDocuments = 0;
                $.each(result['flavourStatistics'], function(index, valueCount) {
                    var dataSetIndex = FLAVOURS[valueCount['value']].dataSetIndex;
                    flavourChartDataset.data[dataSetIndex] = valueCount['count'];
                    flavouredDocuments += valueCount['count'];
                });
                var noneDataSetIndex = FLAVOURS['None'].dataSetIndex;
                flavoursChart.data.datasets[0].data[noneDataSetIndex] = result['totalPdfDocumentsCount'] - flavouredDocuments;
                flavoursChart.update();

                // Versions chart
                versionsChartDataset.data = [];
                $.each(VERSIONS, function() {
                    versionsChartDataset.data.push(0);
                });

                $.each(result['versionStatistics'], function(index, valueCount) {
                    var dataSetIndex = VERSIONS[valueCount['value']].dataSetIndex;
                    versionsChartDataset.data[dataSetIndex] = valueCount['count'];
                });
                versionsChart.update();

                // Producers chart
                var producerChartData = {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: []
                    }]
                };
                $.each(result['topProducerStatistics'], function(index, valueCount) {
                    producerChartData.labels.push(valueCount['value']);
                    producerChartData.datasets[0].data.push(valueCount['count']);
                    producerChartData.datasets[0].backgroundColor.push('white');
                });
                producersChart.data = producerChartData;
                producersChart.update();
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });

    }

    $("#errors-producer-input").on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadErrorsData();
        }
    });

    function loadErrorsData() {
        var url = "/api/report/error-statistics?domain=" + currentDomain.domain;
        var startDate = $("#errors-producer-input")[0].value;
        if (startDate !== "") {
            url += "&startDate=" + startDate;
        }
        //TODO: read flavour, version and producers
        $.ajax({
            url: url,
            type: "GET",
            success: function (result) {
                var errorsChartData = {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: [],
                        borderWidth: 0
                    }]
                };
                var errorsListElement = $('.errors-list');
                errorsListElement.empty();
                $.each(result['topErrorStatistics'], function(index, errorCount) {
                    var error = errorCount['error'];
                    var shortDescription = '';
                    if (error['type'] === 'ruleViolation') {
                        var rule = error['rule'];
                        shortDescription = rule['specification'] + ' ' + rule['clause'] + '-' + rule['testNumber'];
                    } else {
                        shortDescription = 'Generic error #' + error['id'];
                    }
                    var fullDescription = error['description'];
                    var documentsCount = errorCount['count'];
                    var errorColor = ERROR_BACKGROUNDS[index];

                    errorsChartData.labels.push(shortDescription);
                    errorsChartData.datasets[0].data.push(documentsCount);
                    errorsChartData.datasets[0].backgroundColor.push(errorColor);

                    errorsListElement.append(
                        '<div class="error-item d-flex align-items-top">' +
                        '    <span class="material-icons" style="color: ' + errorColor + '">lens</span>' +
                        '    <span class="count">' + documentsCount + '</span>' +
                        '    <div class="error-description">' +
                        '        <div class="short">' + shortDescription + ':</div>' +
                        '        <div class="full">' + fullDescription + '</div>' +
                        '    </div>' +
                        '</div>'
                    );
                });

                errorsChart.data = errorsChartData;
                errorsChart.update();
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });

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

    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        // e.target // newly activated tab
        // e.relatedTarget // previous active tab

        switch (e.target.text) {
            case "Summary":
                loadSummaryData();
                break;
            case "Documents":
                loadDocumentsData();
                break;
            case "Common errors":
                loadErrorsData();
                break;
        }

    });

    $.ajax({
        url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
        type: "GET",
        success: function (result) {
            domainInfoLoaded(result);
            loadSummaryData();
        },
        error: function (result) {
            // reportError("Error on job loading");
        }
    });

    $.ajax({
        url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")) + "/requests",
        type: "GET",
        success: function (result) {
            // domainInfoLoaded(result);

            var mailsList = "";
            for (var i = 0; i < result.length; i++) {
                if (i !== result.length - 1) {
                    if (result[i].emailAddress) {
                        mailsList += result[i].emailAddress + ", ";
                    }
                } else {
                    if (result[i].emailAddress) {
                        mailsList += result[i].emailAddress;
                    }
                }

            }

            $('span.job-mails-list').text(mailsList);
            $('textarea.job-mails-list').val(mailsList);

        },
        error: function (result) {
            // reportError("Error on job loading");
        }
    });

    $("#action-resume").on('click', function () {
        var putData = {};//Object.assign({}, currentDomain);
        putData.domain = currentDomain.domain;
        putData.startTime = currentDomain.startTime;
        putData.finishTime = currentDomain.finishTime;

        putData.status = 'RUNNING';

        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "PUT",
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                $('.main').removeClass('status-' + currentDomain.status.toLowerCase(), { children: true });

                // domainInfoLoaded(result);
                domainInfoLoaded(putData);


            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });

    $("#action-pause").on('click', function () {
        var putData = {};//Object.assign({}, currentDomain);
        putData.domain = currentDomain.domain;
        putData.startTime = currentDomain.startTime;
        putData.finishTime = currentDomain.finishTime;
        putData.status = 'PAUSED';

        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "PUT",
            // async:false,
            data: JSON.stringify(putData),
            headers: { "Content-type": "application/json" },
            success: function (result) {
                $('.main').removeClass('status-' + currentDomain.status.toLowerCase(), { children: true });

                // domainInfoLoaded(result);   
                domainInfoLoaded(putData);

            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });
    });

    $("#action-restart").on('click', function () {
        $.ajax({
            url: "api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")),
            type: "POST",
            success: function (result) {
                // domainInfoLoaded(result);
            },
            error: function (result) {
                // reportError("Error on job loading");
            }
        });

    });



    // Summary tab components

    // TODO: get min from server and calculate yearRange
    var summaryDatePicker = new Pikaday({
        field: document.getElementById('summary-date-input'),
        firstDay: 1,
        maxDate: new Date(),
        yearRange: [new Date().getFullYear() - 10, new Date().getFullYear()],
        showTime: false,
        format: 'YYYY-MM-DD',
        onSelect: loadSummaryData
    });

    var summaryChartContext = document.getElementById("summary-chart").getContext('2d');
    var summaryChart = new Chart(summaryChartContext, {
        type: 'pie',
        data: {
            labels: ["To improve", "Compliant"],
            datasets: [{
                data: [0, 0],
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
        maxDate: new Date(),
        yearRange: [new Date().getFullYear() - 10, new Date().getFullYear()],
        showTime: false,
        format: 'YYYY-MM-DD',
        onSelect: loadDocumentsData
    });

    var flavoursChartContext = document.getElementById("flavours-chart").getContext('2d');
    var flavourChartLabels = [];
    var flavourChartDataset = {
        data: [],
        backgroundColor: []
    };
    $.each(FLAVOURS, function(serverName, uiDescriptor) {
        flavourChartLabels.push(uiDescriptor['displayName']);
        flavourChartDataset.data.push(0);
        flavourChartDataset.backgroundColor.push('white');
    });
    var flavoursChart = new Chart(flavoursChartContext, {
        type: 'bar',
        data: {
            labels: flavourChartLabels,
            datasets: [
                flavourChartDataset
            ]
        },
        options: {
            title: {
                display: true,
                text: 'PDF/A flavours'
            }
        }
    });

    var versionsChartContext = document.getElementById("versions-chart").getContext('2d');
    var versionsChartLabels = [];
    var versionsChartDataset = {
        data: [],
        backgroundColor: []
    };
    $.each(VERSIONS, function(serverName, uiDescriptor) {
        versionsChartLabels.push(uiDescriptor['displayName']);
        versionsChartDataset.data.push(0);
        versionsChartDataset.backgroundColor.push('white');
    });
    var versionsChart = new Chart(versionsChartContext, {
        type: 'bar',
        data: {
            labels: versionsChartLabels,
            datasets: [versionsChartDataset]
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
        maxDate: new Date(),
        yearRange: [new Date().getFullYear() - 10, new Date().getFullYear()],
        showTime: false,
        format: 'YYYY-MM-DD',
        onSelect: loadErrorsData
    });

    var errorsChartContext = document.getElementById("errors-chart").getContext('2d');
    var errorsChart = new Chart(errorsChartContext, {
        type: 'pie'
    });
});