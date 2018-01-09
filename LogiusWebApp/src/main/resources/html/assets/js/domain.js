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
        '#A9A9A9',
        '#9ACD32',
        '#808000',
        '#008B8B',
        '#9400D3',
        '#DEB887',
        '#2F4F4F',
        '#ffffff'
    ];

    // Typeahead settings
    var TYPEAHEAD_LIMIT = 10;

    // Global date picker settings
    var defaultDatePickerOptions = {
        firstDay: 1,
        maxDate: new Date(),
        yearRange: [new Date().getFullYear() - 10, new Date().getFullYear()],
        showTime: false,
        format: 'YYYY-MM-DD'
    };

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
            callback: Chart.Ticks.formatters.linear
        },
        gridLines: {
            color: 'rgba(255, 255, 255, 0.1)',
            zeroLineColor: 'rgba(255, 255, 255, 0.25)'
        }
    });
    Chart.scaleService.updateScaleDefaults('category', {   // x-Axis
        color: 'white',
        ticks: {
            autoSkip: false,
            fontColor: 'white'
        },
        gridLines: {
            color: 'rgba(255, 255, 255, 0.1)',
            zeroLineColor: 'rgba(255, 255, 255, 0.25)'
        }
    });

    // Enable tooltips
    $('[data-toggle="tooltip"]').tooltip();

    var normalizedDomain = normalizeURL(getUrlParameter("domain"));

    // Error handler
    function reportError(response) {
        if (response.responseJSON) {
            $('.domain-error').text(response.responseJSON.message);
        } else {
            $('.domain-error').text(response.responseText);
        }
        enableActions();
    }

    //region Base job information
    var crawlJob;
    var oldStatus;
    function loadCrawlJob() {
        $.get("api/crawl-jobs/" + normalizedDomain).done(function (result) {
            crawlJobLoaded(result);
            loadSummaryData();
        }).fail(reportError);
    }

    function crawlJobLoaded(job) {
        var main = $('.main');
        if (oldStatus) {
            main.removeClass('status-' + oldStatus.toLowerCase());
        }

        crawlJob = job;

        main.addClass('status-' + crawlJob.status.toLowerCase());

        $('.domain-name span').text(crawlJob.domain);

        $('.job-date').text(crawlJob.finished ? 'Tested on ' + crawlJob.startTime + ' - ' + crawlJob.finishTime : 'Test started on ' + crawlJob.startTime);

        $('.status-text').text(crawlJob.status).attr('href', 'domain-status.html?domain=' + crawlJob.domain);

        // TODO: uncomment when emails edit is implemented
        // if (!crawlJob.finished) {
        //     $('.job-mails').addClass('editable');
        // }

        $('.job-mails .label').text(crawlJob.finished ? 'Report sent to:' : 'Send report to:');

        $('a.ods-report-link').attr('href', '/api/report/full.ods?domain=' + crawlJob.domain);

        enableActions();
    }

    function disableActions() {
        $('.action').addClass('disabled');
    }

    function enableActions() {
        $('.action').removeClass('disabled');
    }

    $("#action-resume").on('click', function () {
        if (!crawlJob || $("#action-resume").hasClass('disabled')) return;

        oldStatus = crawlJob.status;
        crawlJob.status = 'RUNNING';

        disableActions();

        $.ajax({
            url: "api/crawl-jobs/" + normalizedDomain,
            type: "PUT",
            data: JSON.stringify(crawlJob),
            headers: { "Content-type": "application/json" },
            success: crawlJobLoaded,
            error: reportError
        });
    });

    $("#action-pause").on('click', function () {
        if (!crawlJob || $("#action-pause").hasClass('disabled')) return;

        oldStatus = crawlJob.status;
        crawlJob.status = 'PAUSED';

        disableActions();

        $.ajax({
            url: "api/crawl-jobs/" + normalizedDomain,
            type: "PUT",
            data: JSON.stringify(crawlJob),
            headers: { "Content-type": "application/json" },
            success: crawlJobLoaded,
            error: reportError
        });
    });

    $("#action-restart").on('click', function () {
        if (!crawlJob || $("#action-restart").hasClass('disabled')) return;

        oldStatus = crawlJob.status;

        disableActions();

        $.ajax({
            url: "api/crawl-jobs/" + normalizedDomain,
            type: "POST",
            success: crawlJobLoaded,
            error: reportError
        });
    });
    //endregion

    //region Crawl job emails
    var errorMessage = '';
    var mailsList = '';
    function loadCrawlRequests() {
        $.get("api/crawl-jobs/" + normalizeURL(getUrlParameter("domain")) + "/requests").done(crawlRequestsLoaded).fail(reportError);
    }

    function crawlRequestsLoaded(requests) {
        // mailsList = '';
        // for (var i = 0; i < requests.length; i++) {
        //     if (i !== requests.length - 1) {
        //         if (requests[i].emailAddress) {
        //             mailsList += requests[i].emailAddress + ", ";
        //         }
        //     } else {
        //         if (requests[i].emailAddress) {
        //             mailsList += requests[i].emailAddress;
        //         }
        //     }
        //
        // }
        //
        // if (mailsList === '') {
        //     mailsList = 'no one';
        // }
        //
        // $('span.job-mails-list').text(mailsList);
        // $('textarea.job-mails-list').val(mailsList);
        console.log(requests);
        var minDate = null;
        for (var i in requests) {
            var crawlSinceTime = requests[i].crawlSinceTime;
            if (crawlSinceTime == null) {
                crawlSinceTime = '2015-01-01';
            }
            if (minDate == null || minDate > crawlSinceTime) {
                minDate = crawlSinceTime;
            }
        }
        if (minDate != null) {
            summaryDateInput.val(minDate);
            documentsDateInput.val(minDate);
            errorsDateInput.val(minDate);
            pdfwamErrorsDateInput.val(minDate);
        }
        loadSummaryData();
    }

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
            var mailsList = validationResult.emails;
            $('span.job-mails-list').text(mailsList.join(', '));
            $('.job-mails').removeClass('edit-mode');
        }
        // TODO: update crawl job requests
    });

    $('.job-mails .cancel').on('click', function () {
        emailTextArea.val(mailsList.join(', ')).removeClass('error').tooltip('hide');
        $('.job-mails').removeClass('edit-mode');
    });
    //endregion

    //region Statistics
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
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
            case "Common PDFWam errors":
                loadPDFWamErrorsData();
                break;
        }
    });

    //region Summary
    var summaryDateInput = $("#summary-date-input");
    var summaryDatePicker = new Pikaday($.extend({}, defaultDatePickerOptions, {
        field: summaryDateInput[0],
        onSelect: loadSummaryData
    }));
    summaryDateInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadSummaryData();
        }
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

    function loadSummaryData() {
        var url = "api/report/summary?domain=" + normalizedDomain;
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
                var openPercent = totalCount === 0 ? 0 : Math.round(openCount * 1000 / totalCount)/10;
                if (openPercent === 0 && openCount > 0) {
                    openPercent = 0.1;
                }
                var notOpenPercent = totalCount === 0 ? 0 : 100 - openPercent;

                $('.summary .good-documents .percent').text(openPercent + '%');
                $('.summary .bad-documents .percent').text(notOpenPercent + '%');

                summaryChart.data.datasets[0].data[0] = notOpenCount;
                summaryChart.data.datasets[0].data[1] = openCount;
                summaryChart.update()
            },
            error: reportError
        });
    }
    //endregion

    //region Documents statistics
    var documentsDateInput = $("#documents-date-input");
    var documentsDatePicker = new Pikaday($.extend({}, defaultDatePickerOptions, {
        field: documentsDateInput[0],
        onSelect: loadDocumentsData
    }));
    documentsDateInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadDocumentsData();
        }
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
    function updateFlavourStatistics(flavourStatistics, totalPdfDocumentsCount) {
        flavourChartDataset.data = [];
        $.each(FLAVOURS, function() {
            flavourChartDataset.data.push(0);
        });

        var flavouredDocuments = 0;
        $.each(flavourStatistics, function(index, valueCount) {
            var dataSetIndex = FLAVOURS[valueCount['value']].dataSetIndex;
            flavourChartDataset.data[dataSetIndex] = valueCount['count'];
            flavouredDocuments += valueCount['count'];
        });
        var noneDataSetIndex = FLAVOURS['None'].dataSetIndex;
        flavoursChart.data.datasets[0].data[noneDataSetIndex] = totalPdfDocumentsCount - flavouredDocuments;
        flavoursChart.update();
    }

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
    function updateVersionStatistics(versionStatistics) {
        versionsChartDataset.data = [];
        $.each(VERSIONS, function() {
            versionsChartDataset.data.push(0);
        });

        $.each(versionStatistics, function(index, valueCount) {
            var dataSetIndex = VERSIONS[valueCount['value']].dataSetIndex;
            versionsChartDataset.data[dataSetIndex] = valueCount['count'];
        });
        versionsChart.update();
    }

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
    function updateTopProducerStatistics(topProducerStatistics) {
        var producerChartData = {
            labels: [],
            datasets: [{
                data: [],
                backgroundColor: []
            }]
        };
        $.each(topProducerStatistics, function(index, valueCount) {
            producerChartData.labels.push(valueCount['value']);
            producerChartData.datasets[0].data.push(valueCount['count']);
            producerChartData.datasets[0].backgroundColor.push('white');
        });
        producersChart.data = producerChartData;
        producersChart.update();
    }

    function loadDocumentsData() {
        var url = "/api/report/document-statistics?domain=" + crawlJob.domain;
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

                // Charts
                updateFlavourStatistics(result['flavourStatistics'], result['totalPdfDocumentsCount']);
                updateVersionStatistics(result['versionStatistics']);
                updateTopProducerStatistics(result['topProducerStatistics']);
            },
            error: reportError
        });

    }
    //endregion

    //region Errors statistics
    var errorsFlavourSelect = $('#errors-flavour-input');
    $.each(FLAVOURS, function(serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(errorsFlavourSelect);
    });
    errorsFlavourSelect.on('change', loadErrorsData);

    var errorsVersionSelect = $('#errors-version-input');
    $.each(VERSIONS, function(serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(errorsVersionSelect);
    });
    errorsVersionSelect.on('change', loadErrorsData);

    var errorsProducerInput = $("#errors-producer-input");
    errorsProducerInput.typeahead({
        minLength: 1,
        highlight: true,
        limit: TYPEAHEAD_LIMIT
    }, {
        name: 'producers',
        source: new Bloodhound({
            remote: {
                url: '/api/document-properties/producer/values?domain=' + normalizedDomain + '&propertyValueFilter=_query_&limit=' + TYPEAHEAD_LIMIT,
                wildcard: '_query_',
                cache: false
            },
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            datumTokenizer: Bloodhound.tokenizers.whitespace
        })
    });
    errorsProducerInput.bind('typeahead:selected', loadErrorsData);
    errorsProducerInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadErrorsData();
        }
    });

    var errorsDateInput = $('#errors-date-input');
    var errorsDatePicker = new Pikaday($.extend({}, defaultDatePickerOptions, {
        field: document.getElementById('errors-date-input'),
        onSelect: loadErrorsData
    }));
    errorsDateInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadErrorsData();
        }
    });

    var errorsChartContext = document.getElementById("errors-chart").getContext('2d');
    var errorsChart = new Chart(errorsChartContext, {
        type: 'pie'
    });

    function loadErrorsData() {
        var url = '/api/report/error-statistics?domain=' + crawlJob.domain;

        var flavour = errorsFlavourSelect.find('option:selected').val();
        if (flavour !== '') {
            url += '&flavour=' + flavour;
        }

        var version = errorsVersionSelect.find('option:selected').val();
        if (version !== '') {
            url += '&version=' + version;
        }

        var producer = errorsProducerInput.typeahead('val');
        if (producer !== '') {
            url += '&producer=' + producer;
        }

        var startDate = errorsDateInput.val();
        if (startDate !== '') {
            url += '&startDate=' + startDate;
        }
        $.ajax({
            url: url,
            type: 'GET',
            success: function (result) {
                var errorsChartData = {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: [],
                        borderWidth: 0
                    }]
                };

                var errorsListElement = $('#errors .errors-list');
                errorsListElement.find('tbody>:not(.template)').remove();

                var template = errorsListElement.find('.template').clone().removeClass('template');
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

                    var element = template.clone();
                    element.find('.count').css('backgroundColor', errorColor).text(documentsCount);
                    element.find('.error-description .short').text(shortDescription);
                    element.find('.error-description .full').text(fullDescription);
                    errorsListElement.append(element);
                });

                errorsChart.data = errorsChartData;
                errorsChart.update();
            },
            error: reportError
        });

    }
    //endregion

    //region PDFWam Errors statistics
    var pdfwamErrorsMap = new Map();
    pdfwamErrorsMap.set('pdfwam.error', {short: 'Error in pdfwam process', link: null});
    pdfwamErrorsMap.set('egovmon.pdf.03', {short: 'Structure Elements (tags)', link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.03'});
    pdfwamErrorsMap.set('egovmon.pdf.05', {short: 'Document Permissions', link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.05'});
    pdfwamErrorsMap.set('egovmon.pdf.08', {short: 'Scanned Document', link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.08'});
    pdfwamErrorsMap.set('wcag.pdf.01', {short: 'Alternative Text for Images', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.01'});
    pdfwamErrorsMap.set('wcag.pdf.02', {short: 'Bookmarks', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.02'});
    pdfwamErrorsMap.set('wcag.pdf.03', {short: 'Correct Tab and Reading Order', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.03'});
    pdfwamErrorsMap.set('wcag.pdf.04', {short: 'Decorative Images', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.04'});
    pdfwamErrorsMap.set('wcag.pdf.06', {short: 'Table Elements', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.06'});
    pdfwamErrorsMap.set('wcag.pdf.09', {short: 'Heading Levels', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.09'});
    pdfwamErrorsMap.set('wcag.pdf.12', {short: 'Form Fields', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.12'});
    pdfwamErrorsMap.set('wcag.pdf.14', {short: 'Running Headers and Footers', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.14'});
    pdfwamErrorsMap.set('wcag.pdf.15', {short: 'Submit Buttons', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.15'});
    pdfwamErrorsMap.set('wcag.pdf.16', {short: 'Natural Language', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.16'});
    pdfwamErrorsMap.set('wcag.pdf.17', {short: 'Page Numbering', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.17'});
    pdfwamErrorsMap.set('wcag.pdf.18', {short: 'Document Title', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.18'});
    pdfwamErrorsMap.set('wcag.pdf.sc244', {short: 'Link Text for External Links', link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.SC244'});
    var pdfwamErrorsFlavourSelect = $('#errors-pdfwam-flavour-input');
    $.each(FLAVOURS, function(serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(pdfwamErrorsFlavourSelect);
    });
    pdfwamErrorsFlavourSelect.on('change', loadPDFWamErrorsData);

    var pdfwamErrorsVersionSelect = $('#errors-pdfwam-version-input');
    $.each(VERSIONS, function(serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(pdfwamErrorsVersionSelect);
    });
    pdfwamErrorsVersionSelect.on('change', loadPDFWamErrorsData);

    var pdfwamErrorsProducerInput = $("#errors-pdfwam-producer-input");
    pdfwamErrorsProducerInput.typeahead({
        minLength: 1,
        highlight: true,
        limit: TYPEAHEAD_LIMIT
    }, {
        name: 'producers',
        source: new Bloodhound({
            remote: {
                url: '/api/document-properties/producer/values?domain=' + normalizedDomain + '&propertyValueFilter=_query_&limit=' + TYPEAHEAD_LIMIT,
                wildcard: '_query_',
                cache: false
            },
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            datumTokenizer: Bloodhound.tokenizers.whitespace
        })
    });
    pdfwamErrorsProducerInput.bind('typeahead:selected', loadPDFWamErrorsData);
    pdfwamErrorsProducerInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadPDFWamErrorsData();
        }
    });

    var pdfwamErrorsDateInput = $('#errors-pdfwam-date-input');
    var pdfwamErrorsDatePicker = new Pikaday($.extend({}, defaultDatePickerOptions, {
        field: document.getElementById('errors-pdfwam-date-input'),
        onSelect: loadPDFWamErrorsData
    }));
    pdfwamErrorsDateInput.on("keydown", function (e) {
        if (e.keyCode === 13) {
            loadPDFWamErrorsData();
        }
    });

    var pdfwamErrorsChartContext = document.getElementById("errors-pdfwam-chart").getContext('2d');
    var pdfwamErrorsChart = new Chart(pdfwamErrorsChartContext, {
        type: 'pie'
    });

    function loadPDFWamErrorsData() {
        var url = '/api/report/pdfwam-statistics?domain=' + crawlJob.domain;

        var flavour = pdfwamErrorsFlavourSelect.find('option:selected').val();
        if (flavour !== '') {
            url += '&flavour=' + flavour;
        }

        var version = pdfwamErrorsVersionSelect.find('option:selected').val();
        if (version !== '') {
            url += '&version=' + version;
        }

        var producer = pdfwamErrorsProducerInput.typeahead('val');
        if (producer !== '') {
            url += '&producer=' + producer;
        }

        var startDate = pdfwamErrorsDateInput.val();
        if (startDate !== '') {
            url += '&startDate=' + startDate;
        }
        $.ajax({
            url: url,
            type: 'GET',
            success: function (result) {
                var errorsChartData = {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: [],
                        borderWidth: 0
                    }]
                };

                var errorsListElement = $('#errors-pdfwam .errors-list');
                errorsListElement.find('tbody>:not(.template)').remove();

                var template = errorsListElement.find('.template').clone().removeClass('template');
                $.each(result, function(index, errorCount) {
                    var errorId = errorCount['id'];
                    var shortDescription = pdfwamErrorsMap.get(errorId).short;
                    var link = pdfwamErrorsMap.get(errorId).link;
                    var documentsCount = errorCount['count'];
                    var errorColor = ERROR_BACKGROUNDS[index];

                    errorsChartData.labels.push(shortDescription);
                    errorsChartData.datasets[0].data.push(documentsCount);
                    errorsChartData.datasets[0].backgroundColor.push(errorColor);

                    var element = template.clone();
                    element.find('.count').css('backgroundColor', errorColor).text(documentsCount);
                    element.find('.error-description .short').text(shortDescription);
                    element.find('.error-description .full').text(errorId);
                    errorsListElement.append(element);
                });

                pdfwamErrorsChart.data = errorsChartData;
                pdfwamErrorsChart.update();
            },
            error: reportError
        });

    }
    //endregion
    //endregion
    //endregion

    //region Main
    loadCrawlJob();
    loadCrawlRequests();
    //endregion
});