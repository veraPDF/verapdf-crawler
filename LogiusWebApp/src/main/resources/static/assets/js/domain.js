$(function () {
    var FLAVOURS = {
        'PDF/A': {
            displayName: 'PDF/A',
            dataSetIndex: 0
        },
        'PDF/UA': {
            displayName: 'PDF/UA',
            dataSetIndex: 1
        },
        'PDF/X': {
            displayName: 'PDF/X',
            dataSetIndex: 2
        },
        'PDF/E': {
            displayName: 'PDF/E',
            dataSetIndex: 3
        },
        'None': {
            displayName: 'None',
            dataSetIndex: 4
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
            //loadSummaryData();
        }).fail(reportError);
    }

    function crawlJobLoaded(job) {
        var main = $('.main');
        if (oldStatus) {
            main.removeClass('status-' + oldStatus.toLowerCase());
        }

        crawlJob = job;

        main.addClass('status-' + crawlJob.status.toLowerCase());
        if (crawlJob.validationEnabled){
            $("#error-nav-pdfwam").removeAttr("style");
            $("#error-nav").removeAttr("style");
        }

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
            headers: {"Content-type": "application/json"},
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
            headers: {"Content-type": "application/json"},
            success: crawlJobLoaded,
            error: reportError
        });
    });

    $("#action-restart").on('click', function () {
        if (!crawlJob || $("#action-restart").hasClass('disabled')) {
            return;
        }

        oldStatus = crawlJob.status;
        disableActions();
        var params = {
            url: "api/crawl-jobs/" + normalizedDomain,
            type: "POST",
            success: crawlJobLoaded,
            error: reportError
        };
        if (localStorage['token']){
            params['headers'] = {'Authorization': 'Bearer ' + localStorage['token']};
        }
        console.log(params);
        $.ajax(params);
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
            return {valid: false};
        } else {
            emailTextArea.removeClass('error').tooltip('hide');
        }
        return {valid: true, emails: emails};
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
            case "Common PDF/A errors":
                loadErrorsData();
                break;
            case "Common PDF/UA errors":
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
        if (summaryDateInput.is(":focus") && e.keyCode === 13) {
            loadSummaryData();
        }
    });

    var summaryChartContext = document.getElementById("summary-chart").getContext('2d');
    var summaryChart = new Chart(summaryChartContext, {
        type: 'pie',
        data: {
            labels: ["PDF documents", "Office Open XML documents", "Microsoft Office documents", "ODF documents"],
            datasets: [{
                data: [0, 0],
                backgroundColor: [
                    '#2F7395',
                    '#9CC0E7',
                    '#CB86A2',
                    '#767D92',
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
                result = result['typeOfDocuments'];
                $('.summary .pdf-documents .pdf').text(result['pdf']);
                $('.summary .oox-office-documents .office').text(result['oo_xml_office']);
                $('.summary .ms-office-documents .office').text(result['ms_office']);
                $('.summary .odf-documents .office').text(result['open_office']);

                // var openCount = result['openDocuments']['pdf'] + result['openDocuments']['office'];
                // var notOpenCount = result['notOpenDocuments']['pdf'] + result['notOpenDocuments']['office'];
                // var totalCount = openCount + notOpenCount;

                var pdfCount = result['pdf'];
                var ooxOfficeCount = result['oo_xml_office'];
                var msOfficeCount = result['ms_office'];
                var openOfficeCount = result['open_office'];
                var totalCount = pdfCount + ooxOfficeCount + msOfficeCount + openOfficeCount;
                totalCount = totalCount === 0 ? 1 : totalCount;

                $('.summary .pdf-documents .percent').text((pdfCount / totalCount * 100).toFixed(1) + '%');
                $('.summary .oox-office-documents .percent').text((ooxOfficeCount / totalCount * 100).toFixed(1) + '%');
                $('.summary .ms-office-documents .percent').text((msOfficeCount / totalCount * 100).toFixed(1) + '%');
                $('.summary .odf-documents .percent').text((openOfficeCount / totalCount * 100).toFixed(1) + '%');

                summaryChart.data.datasets[0].data[0] = pdfCount;
                summaryChart.data.datasets[0].data[1] = ooxOfficeCount;
                summaryChart.data.datasets[0].data[2] = msOfficeCount;
                summaryChart.data.datasets[0].data[3] = openOfficeCount;
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
    $.each(FLAVOURS, function (serverName, uiDescriptor) {
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
                text: 'PDF substandards'
            }
        }
    });

    function updateFlavourStatistics(flavourStatistics, totalPdfDocumentsCount) {
        flavourChartDataset.data = [];
        $.each(FLAVOURS, function () {
            flavourChartDataset.data.push(0);
        });

        var flavouredDocuments = 0;
        $.each(flavourStatistics, function (index, valueCount) {
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
    $.each(VERSIONS, function (serverName, uiDescriptor) {
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
        $.each(VERSIONS, function () {
            versionsChartDataset.data.push(0);
        });

        $.each(versionStatistics, function (index, valueCount) {
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
        $.each(topProducerStatistics, function (index, valueCount) {
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
                //todo delete open and not open?
                $('.documents .total-count').text(result['totalPdfDocumentsCount']);

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
    $.each(FLAVOURS, function (serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(errorsFlavourSelect);
    });
    errorsFlavourSelect.on('change', loadErrorsData);

    var errorsVersionSelect = $('#errors-version-input');
    $.each(VERSIONS, function (serverName, uiDescriptor) {
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
                $.each(result['topErrorStatistics'], function (index, errorCount) {
                    var error = errorCount['error'];
                    var shortDescription = '';
                    var link = null;
                    if (error['type'] === 'ruleViolation') {
                        var rule = error['rule'];
                        shortDescription = rule['specification'] + ' ' + rule['clause'] + '-' + rule['testNumber'];
                        link = 'https://github.com/veraPDF/veraPDF-validation-profiles/wiki/';
                        if (rule['specification'] === 'ISO 19005-1:2005') {
                            link += 'PDFA-Part-1-rules#rule-';
                        } else {
                            link += 'PDFA-Parts-2-and-3-rules#rule-';
                        }
                        link += rule['clause'].split('.').join('') + '-' + rule['testNumber'];
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
                    var shortDesc = element.find('.error-description .short');
                    shortDesc.text(shortDescription + ':');
                    if (link != null) {
                        shortDesc.attr('href', link);
                    }
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
    pdfwamErrorsMap.set('pdfwam.error', {
        short: 'Error in pdfwam process',
        full: null,
        link: null
    });
    pdfwamErrorsMap.set('egovmon.pdf.03', {
        short: 'Structure Elements (tags) [EGOVMON.PDF.03]',
        full: '1.3.1 Info and Relationships “Information, structure, and relationships conveyed through ' +
            'presentation can be programmatically determined or are available in text. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.03'
    });
    pdfwamErrorsMap.set('egovmon.pdf.05', {
        short: 'Document Permissions [EGOVMON.PDF.05]',
        full: '4.1 Compatible “Maximize compatibility with current and future user agents, including ' +
            'assistive technologies.”',
        link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.05'
    });
    pdfwamErrorsMap.set('egovmon.pdf.08', {
        short: 'Scanned Document [EGOVMON.PDF.08]',
        full: '1.4.5 Images of Text “If the technologies being used can achieve the visual presentation, ' +
            'text is used to convey information rather than images of text except for the following: (Level AA)”' +
            '<br/>Customizable: The image of text can be visually customized to the user\'s requirements;' +
            '<br/>Essential: A particular presentation of text is essential to the information being conveyed.',
        link: 'http://checkers.eiii.eu/en/pdftest/EGOVMON.PDF.08'
    });
    pdfwamErrorsMap.set('wcag.pdf.01', {
        short: 'Alternative Text for Images [WCAG.PDF.01]',
        full: '1.1.1 Non-text Content "“All non-text content that is presented to the user ' +
            'has a text alternative that serves the equivalent purpose, except for the situations ' +
            'listed below. (Level A)"<br/>' +
            'Controls, Input<br/>' +
            'Time-Based Media<br/>' +
            'Test<br/>' +
            'Sensory<br/>' +
            'CAPTCHA<br/>' +
            'Decoration, Formatting, Invisible”<br/>',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.01'
    });
    pdfwamErrorsMap.set('wcag.pdf.02', {
        short: 'Bookmarks [WCAG.PDF.02]',
        full: '2.4.5 Multiple Ways "More than one way is available to locate a Web page within a set of ' +
            'Web pages except where the Web Page is the result of, or a step in, a process. (Level AA)"',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.02'
    });
    pdfwamErrorsMap.set('wcag.pdf.03', {
        short: 'Correct Tab and Reading Order [WCAG.PDF.03]',
        full: '1.3.2 Meaningful Sequence: “When the sequence in which content is presented affects its ' +
            'meaning, a correct reading sequence can be programmatically determined. (Level A)”<br/>' +
            '2.1.3 Keyboard (No Exception): “All functionality of the content is operable through ' +
            'a keyboard interface without requiring specific timings for individual keystrokes. (Level AAA)”<br/>' +
            '2.4.3 Focus Order: “If a Web page can be navigated sequentially and the navigation sequences ' +
            'affect meaning or operation, focusable components receive focus in an order that preserves ' +
            'meaning and operability. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.03'
    });
    pdfwamErrorsMap.set('wcag.pdf.04', {
        short: 'Decorative Images [WCAG.PDF.04]',
        full: '1.1.1 Non-text Content "“All non-text content that is presented to the user ' +
            'has a text alternative that serves the equivalent purpose, except for the situations ' +
            'listed below. (Level A)"<br/>' +
            'Controls, Input<br/>' +
            'Time-Based Media<br/>' +
            'Test<br/>' +
            'Sensory<br/>' +
            'CAPTCHA<br/>' +
            'Decoration, Formatting, Invisible”<br/>',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.04'
    });
    pdfwamErrorsMap.set('wcag.pdf.06', {
        short: 'Table Elements [WCAG.PDF.06]',
        full: '1.3.1 Meaningful Sequence: ““Information, structure, and relationships conveyed ' +
            'through presentation can be programmatically determined or are available in text. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.06'
    });
    pdfwamErrorsMap.set('wcag.pdf.09', {
        short: 'Heading Levels [WCAG.PDF.09]',
        full: '1.3.1 Info and Relationships:“Information, structure, and relationships conveyed ' +
            'through presentation can be programmatically determined or are available in text. (Level A)”<br/>' +
            '2.4.1 Bypass Blocks: “A mechanism is available to bypass blocks of content that are repeated ' +
            'on multiple Web pages. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.09'
    });
    pdfwamErrorsMap.set('wcag.pdf.12', {
        short: 'Form Fields [WCAG.PDF.12]',
        full: '1.3.1 Info and Relationships: “Information, structure, and relationships conveyed ' +
            'through presentation can be programmatically determined or are available in text. (Level A)”<br/>' +
            '4.1.2 Name, Role, Value: “For all user interface components (including but not limited to: ' +
            'form elements, links and components generated by scripts), the name and role can be ' +
            'programmatically determined; states, properties, and values that can be set by the user ' +
            'can be programmatically set; and notification of changes to these items is available to user ' +
            'agents, including assistive technologies. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.12'
    });
    pdfwamErrorsMap.set('wcag.pdf.14', {
        short: 'Running Headers and Footers [WCAG.PDF.14]',
        full: '2.4.8 Location: “Information about the user’s location within a set of web ' +
            'pages is available. (Level AAA)”<br/>' +
            '3.2.3 Consistent Navigation: “Navigational mechanisms that are repeated on multiple ' +
            'Web pages within a set of Web pages occur in the same relative order each time they are ' +
            'repeated, unless a change is initiated by the user. (Level AA)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.14'
    });
    pdfwamErrorsMap.set('wcag.pdf.15', {
        short: 'Submit Buttons [WCAG.PDF.15]',
        full: '3.2.2 On Input:“Changing the setting of any user interface component ' +
            'does not automatically cause a change of context unless the user has been advised ' +
            'of the behavior before using the component. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.15'
    });
    pdfwamErrorsMap.set('wcag.pdf.16', {
        short: 'Natural Language [WCAG.PDF.16]',
        full: '3.1.1 Language of Page “The default human language of each Web page can be programmatically ' +
            'determined. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.16'
    });
    pdfwamErrorsMap.set('wcag.pdf.17', {
        short: 'Page Numbering [WCAG.PDF.17]',
        full: '1.3.1 Info and Relationships:“Information, structure, and relationships conveyed ' +
            'through presentation can be programmatically determined or are available in text. (Level A)”<br/>' +
            '3.2.3 Consistent Navigation:“Navigational mechanisms that are repeated on multiple ' +
            'Web pages within a set of Web pages occur in the same relative order each time ' +
            'they are repeated, unless a change is initiated by the user. (Level AA)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.17'
    });
    pdfwamErrorsMap.set('wcag.pdf.18', {
        short: 'Document Title [WCAG.PDF.18]',
        full: '2.4.2 Page Titled “Web pages have titles that describe topic or purpose. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.18'
    });
    pdfwamErrorsMap.set('wcag.pdf.sc244', {
        short: 'Link Text for External Links [WCAG.PDF.SC244]',
        full: '1.3.1 Info and Relationships “Information, structure, and relationships conveyed ' +
            'through presentation can be programmatically determined or are available in text. (Level A)”<br/>' +
            '2.1.1 Keyboard “All functionality of the content is operable through a keyboard interface ' +
            'without requiring specific timings for individual keystrokes, except where the underlying ' +
            'function requires input that depends on the path of the user\'s movement and not just ' +
            'the endpoints. (Level A)”<br/>' +
            '2.4.4 Link Purpose (In Context)“The purpose of each link can be determined from the link ' +
            'text alone or from the link text together with its programmatically determined link context, ' +
            'except where the purpose of the link would be ambiguous to users in general. (Level A)”',
        link: 'http://checkers.eiii.eu/en/pdftest/WCAG.PDF.SC244'
    });
    var pdfwamErrorsFlavourSelect = $('#errors-pdfwam-flavour-input');
    $.each(FLAVOURS, function (serverName, uiDescriptor) {
        $('<option>')
            .val(serverName)
            .text(uiDescriptor.displayName)
            .appendTo(pdfwamErrorsFlavourSelect);
    });
    pdfwamErrorsFlavourSelect.on('change', loadPDFWamErrorsData);

    var pdfwamErrorsVersionSelect = $('#errors-pdfwam-version-input');
    $.each(VERSIONS, function (serverName, uiDescriptor) {
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
                $.each(result, function (index, errorCount) {
                    var errorId = errorCount['id'];
                    var shortDescription = pdfwamErrorsMap.get(errorId).short;
                    var fullDescription = pdfwamErrorsMap.get(errorId).full;
                    var link = pdfwamErrorsMap.get(errorId).link;
                    var documentsCount = errorCount['count'];
                    var errorColor = ERROR_BACKGROUNDS[index];

                    errorsChartData.labels.push(shortDescription);
                    errorsChartData.datasets[0].data.push(documentsCount);
                    errorsChartData.datasets[0].backgroundColor.push(errorColor);

                    var element = template.clone();
                    element.find('.count').css('backgroundColor', errorColor).text(documentsCount);
                    var shortDesc = element.find('.error-description .short');
                    shortDesc.text(shortDescription);
                    if (link != null) {
                        shortDesc.attr('href', link);
                    }
                    element.find('.error-description .full').html(fullDescription);
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