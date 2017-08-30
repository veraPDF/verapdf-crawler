$(function () {
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

    var mailsList = ['example1@report.com', 'example2@report.com']
    var curentDomain = getUrlParameter("domain");
    var correct = true;

    $('#job-date').text((curentDomain==='www.vananaarbeter.nl'||curentDomain==='secure5.svb.nl')?'Tested on 25 Aug 2017, 16:36':'Tested on 25 Aug 2017, 16:36 - 25 Aug 2017, 17:15');
    var setMails = function(){
        if(curentDomain==='www.duallab.com'){
            $('#job-mails').text(mailsList.join(', '));
            $('#job-mails').siblings('b').text('Report sent to:');        
        }else{      
            var t = document.createTextNode(mailsList.join(', '));
            $('#job-mails').append(t);
            var editIcon = $(`<a href="#" class="action edit" >
                                <span class="material-icons"
                                    data-placement="top" title="edit emails" id="edit-icon">create</span>
                            </a>`) ;
            editIcon.children('span').tooltip();
            $('#job-mails').append(editIcon);
            $('#job-mails').siblings('b').text('Send report to:');
        }
    }

    setMails();

    $('#job-mails').on('click', "#edit-icon", (function(){
        $(this).tooltip('dispose');
        var textarea = $("<textarea id='editable-mail-list'></textarea>");
        textarea.append($('#job-mails')[0].firstChild.data);
        textarea.css({"width":"80%", "height":"70px", "resize": "none"});
        $('#job-mails').empty();
        $('#job-mails').css({"width":"600px"})
        $('#job-mails').prepend(textarea);
        var icons = $(`<div class="accept-decline-btn-container">
                            <a href="#" class="action edit" >
                                <span class="material-icons" id="done-icon" 
                                    data-placement="top" title="Apply changes">done</span>
                            </a>
                            <a href="#" class="action edit" >
                                <span class="material-icons" id="clear-icon" 
                                data-placement="top" title="Discard changes">clear</span>
                            </a>
                        </div>`);
        $('#job-mails').append(icons);        
        $('#done-icon').tooltip();   
        $('#clear-icon').tooltip();      
    }));

    $('#job-mails').on('click', "#done-icon", (function(){
        if(correct){
            $(this).tooltip('dispose')
            var content = $('#editable-mail-list')[0].value;
            mailsList = content.split(/\s*,\s*/);
            $('#job-mails').empty();
            setMails();
        }        
    }));

    $('#job-mails').on('click', "#clear-icon", (function(){
        $(this).tooltip('dispose')
        $('#job-mails').empty();
        setMails();                 
    }));
    
    $('#job-mails').on('focusout', '#editable-mail-list', (function() {
        $('#job-mails').children('p').remove()
        var content = $(this)[0].value.split(/\s*,\s*/);
        for(var i = 0; i < content.length; i++){
            if(!content[i].match(/.+?\@.+/g)){
                $('#job-mails').append($('<p style="color: red">you enter incorrect e-mail</p>'));
                correct = false;
                $(this).focus();
                return;
            }
        }
        correct = true;        
    }));
    
    var summaryDatePicker = new Pikaday({
        field: document.getElementById('summary-date-input'),
        firstDay: 1,
        minDate: new Date(2017, 7, 4),
        maxDate: new Date(2020, 12, 31),
        yearRange: [2000,2020],
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

    var documentsDatePicker = new Pikaday({
        field: document.getElementById('documents-date-input'),
        firstDay: 1,
        minDate: new Date(2017, 7, 4),
        maxDate: new Date(2020, 12, 31),
        yearRange: [2000,2020],
        showTime: false,
        format: 'DD-MM-YYYY'
    });

    var flavorsChartContext = document.getElementById("flavors-chart").getContext('2d');
    var flavorsChart = new Chart(flavorsChartContext, {
        type: 'bar',
        data: {
            labels: ['PDF/A-1a', 'PDF/A-1b', 'PDF/A-2a', 'PDF/A-2b','PDF/A-2u', 'PDF/A-3a', 'PDF/A-3b', 'PDF/A-3u', 'Other PDF'],
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
        yearRange: [2000,2020],
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