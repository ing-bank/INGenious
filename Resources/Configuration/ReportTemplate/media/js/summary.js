
var DEBUG = false;
var tSetDetails_ID = ["maxThreads", "runConfiguration", "startTime", "endTime", "exeTime", "nopassTests", "nofailTests"];
var tSetDetails = ["Parallel Threads", "Run Configuration", "Start Time", "End Time", "Total Duration", "Passed Tests", "Failed Tests"];
var CID = ["scenarioName", "testcaseName", "browser", "exeTime", "status", "bversion", "platform", "iterations"];
var colsTitle = ["Scenario", "TestCase", "Browser", "Execution Time", "Status", "Browser Version", "Platform", "Iterations"];
var browsers = [], browsersX = [];
var tSetBrowserDetails = [];
var toggleImg = {
    "true": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAuklEQVQ4T+3SMQ4BQRQG4I9CNDqNA+hcQO0IWiUnEIkjKJxA1AqHULqBygFUKo0okE2GbNauHaE01WT2n+/NvpmKH4/Kjz1/8PuOxvSwilkoNcX1XdkYcIFRQJapea5bBo4xz+yc5Kw9I+/APtZIfjk9bhhglXfEIrCLDeoF/Tqjh232ex7YDsFmyZ0fkRTep3NZsBGwTuQD2gX09MhnwRaSm6xFghcMcSgCI53iWNmz+bjAH/y4ZS8b7qUTFRUHsobWAAAAAElFTkSuQmCC",
    "false": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAACBSURBVDhPYxgFIwAwQmkYkATi5UDMA+YRBp+BOAqIn4N5WAAvEF8B4v9E4ktADNKDF0gB8SMgxmYAMn4AxCAfEQW0gfgDEGMzCITfA7EWEJME3IH4NxCjG/YTiB2AmCyQDMToBiYBMUWgA4hhhrWCBCgFTEDcDcSdUPYoGOGAgQEAwoowLhjiyB4AAAAASUVORK5CYII="
};
var GRPResult = {
    "result": "result", "browser": "browser", "status": "status", "exeTime": "exeTime",
    "bversion": "bversion", "platf": "platform", "iType": "iterationType"
};
var GRPExecolsTitle = ["Scenario", "TestCase"];

var LOG = function(MSG) {
    if (DEBUG)
        console.log(MSG);
};
var LOGE = function(MSG) {
    LOG(MSG.stack);
};
var Util = {
    "contains": function(array, val) {
        return array.indexOf(val) !== -1;
    }
};
function getTitle() {
    if (DATA.testRun) {
        return  DATA.EXECUTIONS[0].scenarioName + ":" + DATA.EXECUTIONS[0].testcaseName;
    } else {
        return  DATA.releaseName + ":" + DATA.testsetName;
    }
}
function getData(){
	function hasActualBDDReport(error){
	return ImageExist('cucumber-html-reports/images/favicon.png',error);
	}
	function ImageExist(url,error){
	var img = new Image();
	img.onerror=error;
	img.src = url;
	}
	DATA.hasBddHtmlReport=true;
	if(DATA.bddReport){
	DATA.bddLink=DATA.bddLink||'cucumber-html-reports/overview-features.html';	
	hasActualBDDReport(function(error){
	DATA.bddLink='data:text/html;charset=utf-8,'+htmlError('Not Found',
		'The BDD Html Report not found!<br/><br/>Please Refer <u>Help/faq/bdd</u>');
	angular.element($('#exeTAB')).scope().$apply();
	});	
	}
	return DATA;
}
function htmlError(title,error){
	return encodeURIComponent( 
        '<!DOCTYPE html>'+
        '<html lang="en">'+
        '<head><title>'+title+'</title></head>'+
        '<body><h2>'+error+'</h2></body>'+
        '</html>'
    )
}

(function() {
    init();
    var app = angular.module('summaryReport', []);
app.config(['$compileProvider', function ($compileProvider) {
    $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|local|data|file):/);
}]);
    app.controller('TestSet', ['$scope', '$sce', function($scope, $sce) {
            $scope.colsTitle = colsTitle;
            $scope.GRPExecolsTitle = GRPExecolsTitle;
            $scope.GRPBrowsers = browsers;
            $scope.GRPBrowsersX = browsersX;
            $scope.tSetDetails = tSetDetails;
            $scope.tSetDetails_ID = tSetDetails_ID;
            $scope.view = 'Single View';
            $scope.NA = "NA";
            $scope.Details = getData();
            $scope.Title = getTitle();
            $scope.Executions = SNGLReport.DATA;
            $scope.GRPExecutions = GRPReport.DATA;
            $scope.getQParamTCLink = getQParamTCLink;
            $scope.getQParamTCLinkAll = getQParamTCLinkAll;
            $scope.getQParamTCLinkFor = getQParamTCLinkFor;

            $scope.getResStatus = function(exe, browser) {
                var res = (exe['result'])[browser.name + browser.ver + browser.platf + browser.iType];
                if (res)
                    return res['status'];
                else
                    return "NA";
            };
            $scope.getResTime = function(exe, browser) {
                var res = (exe['result'])[browser.name + browser.ver + browser.platf + browser.iType];
                if (res)
                    return "" + res['exeTime'] + "";
                else
                    return "";

            };
            $scope.getBrowsersWidth = function(browsers) {
                if (browsers.length > 7) {
                    return "85";
                } else {
                    return browsers.length * 7;
                }
            };
        }]);


})();

var getQParamTCLink = function(eData) {
    var tc = eData.testcaseName;
    var sc = eData.scenarioName;
    var bro = eData.browser;
    var brov = eData.bversion;
    var platf = eData.platform;
    var iType = eData.iterationType;

    return "SC=" + encodeURIComponent(sc) + "&TC=" + encodeURIComponent(tc) + "&BRO="
            + encodeURIComponent(bro) + "&BROV=" + encodeURIComponent(brov) + "&PLATF=" + encodeURIComponent(platf) + "&ITYPE=" + encodeURIComponent(iType);
};
var getQParamTCLinkAll = function(eData) {
    var tc = eData[colsTitle[1]];
    var sc = eData[colsTitle[0]];
    return "SC=" + encodeURIComponent(sc) + "&TC=" + encodeURIComponent(tc) + "&BRO=ALL";
};
var getQParamTCLinkFor = function(eData, bro) {
    var tc = eData[colsTitle[1]];
    var sc = eData[colsTitle[0]];
    return "SC=" + encodeURIComponent(sc) + "&TC=" + encodeURIComponent(tc) + "&BRO="
            + encodeURIComponent(bro.name) + "&BROV=" + encodeURIComponent(bro.ver) + "&PLATF=" + encodeURIComponent(bro.platf) + "&ITYPE=" + encodeURIComponent(bro.iType);
};

function init() {

}

var prepareGRPExeData = function() {
    var GRPExes = [];
    collectBrowsers();
    function getBroUID(row) {
        return row[GRPResult.browser] + row[GRPResult.bversion] + row[GRPResult.platf] + row[GRPResult.iType];
    }
    DATA.EXECUTIONS.forEach(function(row) {
        var stat = {};
        stat[ GRPResult.status] = row[GRPResult.status];
        stat[ GRPResult.exeTime] = row[GRPResult.exeTime];
        var eindex = indexOFTestCase(GRPExes, row[CID[0]], row[CID[1]]);
        if (eindex === -1)
        {
            var exe = {};
            GRPExecolsTitle.forEach(function(col) {
                exe[col] = row[CID[colsTitle.indexOf(col)]];
            });
            var res = {};
            res[getBroUID(row)] = stat;
            exe[GRPResult.result] = res;
            GRPExes.push(exe);
        } else {

            var exe = GRPExes[eindex];
            var result = exe[GRPResult.result];
            result[getBroUID(row)] = stat;
            exe[GRPResult.result] = result;
            GRPExes[eindex] = exe;
        }

    });

    return GRPExes;

};

var indexOFTestCase = function(exes, sc, tc) {
    var found = -1;
    exes.forEach(function(row, index) {
        if (row[colsTitle[0]] === sc)
            if (row[colsTitle[1]] === tc) {
                found = index;
                return;
            }
    });
    return found;
};

var collectBrowsers = function() {
    DATA.EXECUTIONS.forEach(function(row) {
        var profile = {};
        profile.name = row.browser;
        profile.ver = row.bversion;
        profile.platf = row.platform;
        profile.iType = row.iterationType;
        if (!hasentry(profile))
        {
            browsers.push(profile);
        }
        if (!hasentryX(profile, row.status)) {
            var profileX = profile;
            profileX.res = {};
            profileX.res.no = 1;
            if (row.status === 'PASS') {
                profileX.res.pass = 1;
                profileX.res.fail = 0;
            } else {
                profileX.res.pass = 0;
                profileX.res.fail = 1;
            }
            browsersX.push(profileX);
        }
    });
    function hasentry(pro) {
        var exist = false;
        browsers.forEach(function(data) {
            if (data.name === pro.name && data.ver === pro.ver
                    && data.platf === pro.platf && data.iType === pro.iType)
            {
                exist = true;
                return true;
            }
        });
        return exist;
    }

    function hasentryX(pro, status) {
        var exist = false;
        browsersX.forEach(function(data) {
            if (data.name === pro.name && data.ver === pro.ver
                    && data.platf === pro.platf)
            {
                exist = true;
                data.res.no++;
                if (status === 'PASS') {
                    data.res.pass++;
                } else {
                    data.res.fail++;
                }
                return true;
            }
        });
        return exist;
    }
};
var setSNGLExeTab = function() {
    $('#exeTAB').DataTable({
        "paging": false,
        "ordering": true,
        "info": false
    });
    var table = $('#exeTAB').DataTable();
    var colvis = new $.fn.dataTable.ColVis(table, {
        buttonText: 'show / hide columns',
        exclude: [0, 1, 2, 3, 4],
        align: "left"
    });
    $(colvis.button()).insertAfter('#exeTAB_filter label');
    setupSNGLExeFilter(table);
};
var setupSNGLExeFilter = function(table) {
    try {
        $('#exeTAB thead th').each(function() {
            var title = $(this).text();
            $(this).append('<br><input class = "hideOnPrint" type="text" placeholder="Search ' + title + '"/>');
        });
        table.columns().eq(0).each(function(colIdx) {
            $('input', table.column(colIdx).header()).on('keyup change', function() {
                table.column(colIdx).search(this.value).draw();
            });
        });
        $("#exeTAB").wrap("<div class='exe table wrapper'></div>");
    } catch (ex) {
        LOG(ex);
    }
};
var setGRPExeTab = function() {
    // DataTable
    var table = $('#exeTABGRP').DataTable({
        scrollCollapse: true,
        paging: false,
        ordering: true,
        info: false
    });
    var colvis = new $.fn.dataTable.ColVis(table, {
        buttonText: 'show / hide columns',
        exclude: [0, 1],
        align: "left"
    });
    $(colvis.button()).insertAfter('#exeTABGRP_filter label');
    setupGRPExeFilter(table);

};
var setupGRPExeFilter = function(table) {

    $('#exeTABGRP thead th').each(function() {
        var title = $(this).text().trim();
        $(this).append('<br><input class = "hideOnPrint" type="text" placeholder="Search ' + title + '" />');
    });
    table.columns().indexes().each(function(idx) {
        $('input', table.column(idx).header()).on('keyup change', function() {
            table.column(idx).search(this.value).draw();
        });
    });
    try {
        $("#exeTABGRP").wrap("<div class='exe table wrapper'></div>");
    } catch (ex) {
        LOG(ex);
    }
};

var SNGLReport = {
    "setupTable": setSNGLExeTab,
    "setupFilter": setupSNGLExeFilter,
    "DATA": DATA.EXECUTIONS
};
var GRPReport = {
    "setupTable": setGRPExeTab,
    "setupFilter": setupGRPExeFilter,
    "DATA": prepareGRPExeData()
};

function scrollToTop() {
    $(window).scroll(function() {
        if ($(this).scrollTop() > 100) {
            // $('.scrollToTop').fadeIn();
            $('.scrollToTop').css('background-image', 'url(' + toggleImg.true + ')');
            $('.scrollToTop').attr('goTo', 0);
        } else {
            $('.scrollToTop').css('background-image', 'url(' + toggleImg.false + ')');
            $('.scrollToTop').attr('goTo', $(document).height());
        }
    });
    $('.scrollToTop').click(function() {
        $('html, body').animate({scrollTop: $('.scrollToTop').attr('goTo')}, 800);
        return false;
    });
    $('.scrollToTop').css('background-image', 'url(' + toggleImg.false + ')');
    $('.scrollToTop').attr('goTo', $(document).height());
}
var DropDown = function() {
    var sel = $('select');
    var setv = function(e) {
        var scope = angular.element(sel).scope();
        scope.$apply(function() {            
            scope.view = e.target.selectedOptions[0].label;
			LOG(scope.view);
        });
    };
    sel.select2();
    sel.on("change", setv);

};
$(document).ready(function() {
    scrollToTop();
    SNGLReport.setupTable();
    GRPReport.setupTable();
    DropDown();

});
