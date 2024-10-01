
var DEBUG = false;
var escRule = {};
var Params;
var GRP = "ALL";
var ID = {"scname": "scenarioName", "name": "testcaseName", "stime": "startTime", "etime": "endTime",
    "stat": "status", "exetime": "exeTime", "browser": "browser", "iterations": "iterations",
    "bversion": "bversion", "platform": "platform", "iType": "iterationType", "emode": "runConfiguration",
    "STEP": {"no": "stepno", "name": "stepName", "action": "action", "desc": "description", "status": "status", "time": "tStamp", "res": "result", "link": "link", "objects": "objects", "actual": "actual", "expected": "expected", "comparison": "comparison"}};
var tcDetails_ID = [ID.stat, ID.browser, ID.bversion, ID.platform, ID.stime, ID.etime, ID.exetime, "nopassTests", "nofailTests"];
var tcDetails = ["Overall Status", "Browser", "Browser Version", "Platform", "Start Time", "End Time", "Total Duration", "Passed Steps", "Failed Steps"];
var exeDetails = ["Step No", "Step Name", "Description", "Status", "Time"];
var exeDetails_GRP = ["Step No", "Step Name", "Description"];
var exeDetails_ID = [ID.STEP.no, ID.STEP.name, ID.STEP.desc, ID.STEP.status, ID.STEP.time];
var views = ["ALL"];
var browserHeaders = [];
var browserDetails = {};
var level = {"iteration": "iteration", "reusable": "reusable", "step": "step"};
var toggleImg = {
    "true": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAuklEQVQ4T+3SMQ4BQRQG4I9CNDqNA+hcQO0IWiUnEIkjKJxA1AqHULqBygFUKo0okE2GbNauHaE01WT2n+/NvpmKH4/Kjz1/8PuOxvSwilkoNcX1XdkYcIFRQJapea5bBo4xz+yc5Kw9I+/APtZIfjk9bhhglXfEIrCLDeoF/Tqjh232ex7YDsFmyZ0fkRTep3NZsBGwTuQD2gX09MhnwRaSm6xFghcMcSgCI53iWNmz+bjAH/y4ZS8b7qUTFRUHsobWAAAAAElFTkSuQmCC",
    "false": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAACBSURBVDhPYxgFIwAwQmkYkATi5UDMA+YRBp+BOAqIn4N5WAAvEF8B4v9E4ktADNKDF0gB8SMgxmYAMn4AxCAfEQW0gfgDEGMzCITfA7EWEJME3IH4NxCjG/YTiB2AmCyQDMToBiYBMUWgA4hhhrWCBCgFTEDcDcSdUPYoGOGAgQEAwoowLhjiyB4AAAAASUVORK5CYII="
};
var logFileLoc="./logs/";
/**
 * prototype for String.replaceAll()
 * @param {type} find string to replace
 * @param {type} replace replacement
 * @returns {String.prototype.replaceAll.replace}
 */
String.prototype.replaceAll = function(find, replace) {
    return this.replace(new RegExp(
            find.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'g')
            , replace);
};
/**
 *String prototye to escape html tags
 * @returns {String.prototype@call;replaceAll@call;replaceAll}
 */
String.prototype.escapeTags = function() {
    if (this.contains("#CTAG"))
        return this.replaceAll("#CTAG", "");
    else
        return this.replaceAll("<", "&lt")
                .replaceAll(">", "&gt");


};
/**
 * return string contains substring
 * @param {type} str
 * @returns {Boolean}
 */
String.prototype.contains = function(str) {
    return this.indexOf(str) !== -1;
};
/**
 * String prototye to escape special chars in dynamic id
 * @returns {String.prototype@call;replaceAll@call;replaceAll}
 */
String.prototype.escape = function() {

    return  this.replaceAll(" ", "--_SPACE_--")
            .replaceAll(":", "--_COLN_--").replaceAll("@", "--_AT_--")
            .replaceAll("#", "--_HASH_--").replaceAll("$", "--_DOL_--")
            .replaceAll(",", "--_COMM_--").replaceAll(".", "--_DOT_--")
            .replaceAll("'", "--_SQT_--").replaceAll("!", "--_ES_--")
            .replaceAll("(", "--_OPN_--").replaceAll(")", "--_CLS_--")
            .replaceAll("+", "--_PLS_--").replaceAll("=", "--_EQ_--")
            .replaceAll("[", "--_OPN1_--").replaceAll("]", "--_CLS1_--")
            .replaceAll("{", "--_OPN2_--").replaceAll("}", "--_CLS2_--")
            .replaceAll("^", "--_CAR_--").replaceAll(";", "--_SEM_--")
            .replaceAll("%", "--_PER_--").replaceAll("&", "--_AMP_--");

};

var getTabHeader = function() {
    var head = " <colgroup><col style='width: 8%'/><col style='width: 17%'/>" +
            "<col style='width:40%'/><col style='width: 10%'/><col style='width: 15%'/>" +
            "</colgroup> <thead align=\"center\" class='exe table'><tr>";
    exeDetails.forEach(function(col) {
        head += " <th>" + col + "</th>";
    });
    head += "</tr></thead>";
    return head;
};
var tabHeaderGRP = function() {
    var head = "<thead align=\"center\" class='exe table'><tr>";
    head += " <th style='min-width: 60px;width:10%;'>" + exeDetails_GRP[0] + "</th>";
    head += " <th style='min-width: 150px;width:15%;'>" + exeDetails_GRP[1] + "</th>";
    head += " <th style='min-width: 300px;width:30%;'>" + exeDetails_GRP[2] + "</th>";
    browserHeaders.forEach(function(col) {
        head += " <th style='min-width: 120px;width:15%;'>" + browserDetails[col].name
                + "-" + browserDetails[col].ver + " <small> " + browserDetails[col].platf + "</small></th>";
    });

    head += "</tr></thead>";
    return head;
};
var tabHeaderSNGL = getTabHeader();
(window.onpopstate = function() {

    var match,
            pl = /\+/g,
            search = /([^&=]+)=?([^&]*)/g,
            decode = function(s) {
                return decodeURIComponent(s.replace(pl, " "));
            },
            query = '';
            var regex = /^\?[^#]*$/;
            var searchString = window.location.search;
            if (regex.test(searchString)) {
                query = searchString.substring(1);
            }
    Params = {};
    while (match = search.exec(query))
        Params[decode(match[1])] = decode(match[2]);
})();
var LOG = function(MSG) {
    if (DEBUG)
        console.log(MSG);
};
var LOGE = function(MSG) {
    LOG(MSG.stack);
};
var toggle = function(ele) {
    if (ele.is(":visible")) {
        ele.fadeOut('fast');
    } else {
        ele.fadeIn('normal');
    }
    //ele.toggle();
};
var contains = function(array, val) {
    return array.indexOf(val) !== -1;
};
var checkandPush = function(array, val) {
    if (!contains(array, val))
        array.push(val);
    return array;
};
var Util = {
    "contains": contains,
    "checkandPush": checkandPush
};
var setShow = function(key) {
    visibleMap[key] = !(visibleMap[key] || false);
};
var isTCMatched = function(exe) {
    return (exe[ID.scname] === Params.SC) && (exe[ID.name] === Params.TC);
};
(function() {

    var app = angular.module('detailedReport', []);
    app.controller('TestCase', ['$scope', '$sce', function($scope, $sce) {
            $scope.GRP = GRP;
            $scope.Details = getExeData();
			$scope.perfReport = DATA.perfReport;
            $scope.GRPSteps = getGRPExedata($scope.Details);
            $scope.tcDetails = tcDetails;
            $scope.tcDetails_ID = tcDetails_ID;
            $scope.exeCols = exeDetails;
            $scope.exeCols_ID = exeDetails_ID;

            $scope.setDefView = function() {
                if (Params.BRO === GRP)
                    return (Params.BRO);
                else
                    return (Params.BRO + " " + Params.BROV + " " + Params.PLATF + " " + Params.ITYPE);
            };
            $scope.view = $scope.setDefView();
            $scope.browsers = browserHeaders;
            $scope.views = views;
            $scope.Title = Params.SC + " : " + Params.TC;
		logFileLoc=logFileLoc+Params.SC + "_" + Params.TC+".txt";
            $scope.cDetails = ($scope.Details[$scope.view]);
            $scope.cRowsGRP = $sce.trustAsHtml(renderTableGRP($scope.GRPSteps));
            $scope.setView = function(v) {
                $scope.view = v;
                $("select option").filter(function() {
                    return $(this).text() === v;
                }).prop('selected', true);
            };
            $scope.getWidth = function() {
                var size = ($scope.browsers.length + 1) * 10;
                return size;
            };
            $scope.getSNGLExedata = function(data) {
                var tabs = {};
                browserHeaders.forEach(function(browser) {
                    var cData = data[browser];
                    var cTab = renderTableSNGL(cData, browser);
                    tabs[browser] = $sce.trustAsHtml(cTab);
                });
                LOG(tabs);
                return tabs;
            };

            $scope.SNGLTabs = ($scope.getSNGLExedata($scope.Details));
            $scope.$watchCollection('view', function(n, o) {
                if (n !== GRP) {
                    $scope.cDetails = ($scope.Details[n]) || ($scope.Details[o]);
                } else {
                    // onResized();
                }
            });
            $scope.videoReport = DATA.videoReport;
            $scope.steps = $scope.Details[$scope.view].STEPS;
            $scope.myLink = $scope.steps.length == 1 ? $scope.steps[0].videoReportDir : 'videoReport.html?SC='+ $scope.Details[$scope.view].scenarioName +'&TC='+ $scope.Details[$scope.view].testcaseName;
        }]);
})();
var setCImage = function(url) {
    $('#lightBoxImg').attr('src', url);
    showTooltip();
};
var toggleIteration = function(view, key) {
    try {
        var imag = $('#' + view + key + '-toggle');
        var visible = imag.attr("src") === toggleImg['false'];
        $('#' + view + key + '-toggle').attr("src", toggleImg[visible]);
        $('#' + view + key).nextAll('tr').each(function() {
            var ele = $(this);
            if (ele.attr("iteration") === key) {// part of current iteration only
                if (ele.attr("level") === "step") { 
                    if (ele.attr("reusable") === "") { // if its a top level steo (not under reusable) toggle it 
                        toggle(ele);
                    } else {//Eliminating the wrong combinations to get the other
                        if ((visible && !ele.is(":visible")) ||
                                (!visible && ele.is(":visible"))) {
                            toggle(ele);
                        }
                    }
                } else if (ele.attr("level") === "reusable") {
                    if ((ele.attr("reusable") === "")) {//if under iteration toggle (top level reusable) toggle it
                        toggle(ele);
                        $('#' + ele.attr('id') + '-toggle').attr("src", toggleImg[visible]);
                    }
                    else { // if its under any reusable .., Eliminating the wrong combinations to get the other
                        if ((visible && !ele.is(":visible")) ||
                                (!visible && ele.is(":visible"))) {
                            toggle(ele);
                            $('#' + ele.attr('id') + '-toggle').attr("src", toggleImg[visible]);
                        }
                    }
                }
            } else {
                return false;
            }
        });
    } catch (ex) {
        LOG("ERROR toggle Steps for  : " + key + " in view " + view + "\n");
        LOGE(ex);
    }
};
var toggleReusable = function(view, key) {
    try {

        var imag = $('#' + view + key + '-toggle');
        var visible = imag.attr("src") === toggleImg['false'];
        var elist = [];
        $('#' + view + key).nextAll('tr').each(function() {
            var ele = $(this);
            if (ele.attr("level") !== "iteration") {
                if (ele.attr("reusable") === key)// only children 
                {
                    if (ele.attr("level") === "reusable") {
                        var nkey = ele.attr("id").replace(view, "");
                        var reState = ($('#' + view + nkey + '-toggle').attr("src") === toggleImg['false']);
                        //Eliminating the wrong combinations to get the other
                        if ((visible && !reState) || (reState !== !visible)) {
                            toggleReusable(view, nkey);//recursive call to handle reusable (inside reusable)* toggle
                        }
                    }//adding it to the list for later process
                    elist.push(ele);
                }
            } else {
                return false;
            }
        });
        imag.attr("src", toggleImg[visible]);
        elist.forEach(function(e) {
            toggle(e);
        });
    } catch (ex) {
        LOG("ERROR toggle Steps for  : " + key + " in view " + view + "\n");
        LOGE(ex);
    }
};

function getBroUID(row, sep) {
    return row[ID.browser] + sep + row[ID.bversion] + sep + row[ID.platform] + sep + row[ID.iType];
}
var getExeData = function() {
    var det = {};
    var eList = DATA.EXECUTIONS;
    try {
        eList.forEach(function(exe) {
            if (isTCMatched(exe)) {
                var broID = getBroUID(exe, " ");
                det[broID] = exe;
                views = Util.checkandPush(views, broID);
                browserHeaders = Util.checkandPush(browserHeaders, broID);
                var profile = {};
                profile.name = exe[ID.browser];
                profile.ver = exe[ID.bversion];
                profile.platf = exe[ID.platform];
                profile.iType = exe[ID.iType];
                browserDetails[broID] = profile;
            }
        });
        if (Params.BRO !== GRP && !det[Params.BRO + " " + Params.BROV + " " + Params.PLATF + " " + Params.ITYPE]) {
            Params.BRO = GRP;
        } else {

        }
    } catch (ex) {
        LOG("ERROR Parsing Execution Data : \n");
        LOGE(ex);
    }
    LOG(det);
    
    return det;
};
function renderTableSNGL(data, browser) {
    var browserID = browser.escape();
    function getRowHtm(row) {
		try{
        var stat = row[exeDetails_ID[3]];
		if(stat=="COMPLETE"){
			var flag=1;
			if (row[exeDetails_ID[1]].charAt(0)=='p'){ //put, post
				flag=0;
			}
			var statElem = setStatusLinkWeb(stat, row[exeDetails_ID[0]], row['link'],flag);
		} else{
        var statElem = setStatusLink(stat, row['actual'], row['expected'], row['comparison'], row['objects'], row['link']);
		}
        var data = "<td class='exe table col stepno'>" + row[exeDetails_ID[0]] + "</td>";
        data += "<td >" + row[exeDetails_ID[1]] + "</td>";
        data += "<td>" + (row[exeDetails_ID[2]]).escapeTags() + "</td>";
        data += "<td style='width: 10%' class='exe table " + stat + "'>" + statElem + "</td>";
        data += "<td style='width: 15%' class='exe table time'>" + row[exeDetails_ID[4]] + "</td>";
		}catch(ex){
		console.log("Error "+ data);
		console.log(ex);
	}
        return   data;
    }
    function getRClass(step) {
        return  (parseInt(step.data[ID.STEP.no]) % 2) === 1 ? 'oddx' : 'evenx';
    }
    function getStep(step, reusable, iteration) {
        var body = "<tr class='exe table step " + getRClass(step) + "' level='step' iteration='" + iteration
                + "' reusable='" + reusable + "'>";
        body += getRowHtm(step.data);
        body += "</tr>";
        return body;
    }
    function getiterationBody(steps, iterationname, uidx) {
        var iName = iterationname;
        var iId = (iName).escape();
        try {
            if (steps.type === 'reusable')
            {
                var rName = steps.name;
                var rId = (rName).escape();
                var uid = uidx + iId + "-" + rId;
                var body = "<tr class='exe table reusable' level='reusable' id='SNGL" + browserID + uid + "' iteration='"
                        + iId + "' reusable='" //reusable attr added to handle reusable (inside reusable)* toggle
                        + uidx + "'><td/><td/><td   onclick=\"toggleReusable('SNGL" + browserID + "','" + uid + "')\"><center> "
                        + steps.name + "<img id='SNGL" + browserID + uid + "-toggle' src='" + toggleImg.true
                        + "'></img></center></td><td style='width: 10%' class='exe table " +
                        steps.status + "'>" + steps.status + "</td><td/></tr>";
                steps.data.forEach(function(step) {
                    body += getiterationBody(step, iName, uid);//recursive call to handle reusable (inside reusable)*
                });
                return body;
            } else if (steps.type === 'step') {
                return getStep(steps, uidx, iId);// added combined uidx to handle toggle 
            }
        } catch (ex) {
            LOG("ERROR Parsing Single Steps for : " + uid + "\n");
            LOGE(ex);
        }
    }

    function getIteration(iteration) {
        var iterationBody = "";
        var uid = (iteration.name).escape();
        try {
            var iterationHead = "<tr class='exe table iteration' id='SNGL" + browserID + uid
                    + "' level='iteration' ><td/><td/><td onclick=\"toggleIteration('SNGL" + browserID + "','"
                    + uid + "')\"><center>" + iteration.name + "<img id='SNGL" + browserID + uid + "-toggle' src='"
                    + toggleImg.true + "'></img></center></td><td style='width: 10%' class='exe table " +
                    iteration.status + "'>" + iteration.status + "</td><td/></tr>";
            iteration.data.forEach(function(r) {
                iterationBody += getiterationBody(r, iteration.name, "");
            });
        } catch (ex) {
            LOG("ERROR Parsing Single View iteration  : " + uid + "\n");
            LOGE(ex);
        }
        return iterationHead + iterationBody;
    }
    function renderTable(data) {
        var row = tabHeaderSNGL;
        row += "<tbody class='exe table'>";
        try {
            data.STEPS.forEach(function(iteration) {
                row += getIteration(iteration, browser);
            });
        } catch (ex) {
            LOG("ERROR Parsing Single View Steps for  : " + browser + "\n");
            LOGE(ex);
        }
        return row + "</tbody>";
    }
    return renderTable(data);
}
function renderTableGRP(data) {
    function getRowHtm(row) {
	try{
        var data = "<td class='exe table col stepno'>" + row[exeDetails_ID[0]] + "</td>";
        data += "<td >" + row[ID.STEP.action] + "</td>";
        data += "<td>" + (row[exeDetails_ID[2]]).escapeTags() + "</td>";
        browserHeaders.forEach(function(view) {
            var res = (row[ID.STEP.res])[view];
            var statVal = "<small>N/A</small>";
            if (res) {
                var stat = res[ID.STEP.status];
                var statElem = setStatusLink(stat, res['actual'], res['expected'], res['comparison'], res['objects'], res['link']);
                statVal = statElem + "<br><lable class='exe table time'>  " + res[ID.STEP.time] + "<lable>";
            }
			
            data += "<td style='width: 10%' class='exe table res'>" + statVal + "</td>";
        });
	}catch(ex){
		LOG("Error "+ data);
		LOGE(ex);
	}
        return   data;
    }

    function getRClass(step) {
        return  (parseInt(step.data[ID.STEP.no]) % 2) === 1 ? 'oddx' : 'evenx';
    }
    function getStep(step, reusable, iteration) {
        var body = "<tr class='exe table step " + getRClass(step) + "' level='step' iteration='" + iteration
                + "' reusable='" + reusable + "'>";
        body += getRowHtm(step.data);
        body += "</tr>";
        return body;
    }
    function getDCols(data) {
        var col = "";
        browserHeaders.forEach(function(view) {
            col += "<td style='width: 10%' class='exe table " +
                    (data.status[view] || "DONE") + "'>" + (data.status[view] || "") + "</td>";
        });
        return col;
    }
    function getiterationBody(steps, iterationname, uidx) {
        var iName = iterationname;
        var iId = (iName).escape();
        try {
            if (steps.type === 'reusable')
            {
                var rName = steps.name;
                var rId = (rName).escape();
                var uid = uidx + iId + "-" + rId;
                var body = "<tr class='exe table reusable' level='reusable' id='GRP" + uid + "' iteration='"
                        + iId + "' reusable='" //reusable attr added to handle reusable (inside reusable)* toggle
                        + uidx + "'><td/><td/><td   onclick=\"toggleReusable('GRP','" + uid + "')\"><center> "
                        + steps.name + "<img id='GRP" + uid + "-toggle' src='" + toggleImg.true
                        + "'></img></center></td>" + getDCols(steps) + "</tr>";
                var nSteps = steps.data;              
                nSteps.forEach(function(step) {
                    body += getiterationBody(step, iName, uid);//recursive call to handle reusable (inside reusable)*
                });
                return body;
            } else if (steps.type === 'step') {
                return getStep(steps, uidx, iId); // added combined uidx to handle toggle 
            }
        } catch (ex) {
            LOG("ERROR Parsing GroupView Iteration : " + iName + "\n");
            LOGE(ex);
            return "";
        }
    }
    function getIteration(iteration) {
        var iterationBody = "";
        var uid = (iteration.name).escape();
        try {
            var iterationHead = "<tr class='exe table iteration' id='GRP" + uid
                    + "' level='iteration' ><td/><td/><td onclick=\"toggleIteration('GRP','"
                    + uid + "')\"><center>" + iteration.name + "<img id='GRP" + uid + "-toggle' src='"
                    + toggleImg.true + "'></img></center></td>" + getDCols(iteration) + "</tr>";
            iteration.data.forEach(function(r) {
                iterationBody += getiterationBody(r, iteration.name, "");
            });
        } catch (ex) {
            LOG("ERROR Parsing GroupView Iterations : \n");
            LOGE(ex);
        }
        return iterationHead + iterationBody;
    }
    function renderTable(data) {
        var row = tabHeaderGRP();
        row += "<tbody class='exe table'>";
        try {
            data.forEach(function(iteration) {
                row += getIteration(iteration);
            });
        } catch (ex) {
            LOG("ERROR Parsing GroupView Data : \n");
            LOGE(ex);
        }
        return row + "</tbody>";
    }
    return renderTable(data);
}
var setSNGLExeTab = function(browser) {
    try {

        var table = $('#exeTABSNGL' + browser).DataTable({
            "paging": false,
            "ordering": false,
            "info": false
        });
        setupSNGLExeFilter(table, browser);
    } catch (ex) {
        LOG("ERROR Init. DataTable for : " + browser + " \n");
        LOGE(ex);
    }
};
var setupSNGLExeFilter = function(table, browser) {
    $('#exeTABSNGL' + browser + ' thead th').each(function() {
        var title = $(this).text();
        $(this).append('<br><input class = "hideOnPrint" type="text" placeholder="Search ' + title + '"/>');
    });
    table.columns().eq(0).each(function(colIdx) {
        $('input', table.column(colIdx).header()).on('keyup change', function() {
            table.column(colIdx).search(this.value).draw();
        });
    });
};
var setGRPExeTab = function() {

    var table = $('#exeTABGRP').DataTable({
        "paging": false,
        "ordering": false,
        "info": false
    });
    var colvis = new $.fn.dataTable.ColVis(table, {
        buttonText: 'show / hide columns',
        exclude: [0, 1, 2],
        align: "left"
    });
    $(colvis.button()).insertAfter('#exeTABGRP_filter label');
    setupGRPExeFilter(table);
    $("#exeTABGRP").wrap("<div class='exe table wrapper'></div>");
};
var setupGRPExeFilter = function(table) {
    try {
        $('#exeTABGRP thead th').each(function() {
            var title = $(this).text();
            $(this).append('<br><input class = "hideOnPrint" type="text"  placeholder="Search ' + title + '"/>');
        });
//	Log file
	document.getElementById("logs").setAttribute("href",logFileLoc);
        table.columns().indexes().each(function(colIdx) {
            $('input', table.column(colIdx).header()).on('keyup change', function() {
                table.column(colIdx).search(this.value).draw();
            });
        });
    } catch (ex) {
        LOGE(ex);
    }


};
function getGRPExedata(data) {
    var STEPS = [];
    function getIterationProto(iteration) {
        var n_iteration = {};
        n_iteration.type = level.iteration;
        n_iteration.name = iteration.name;
        n_iteration.status = {};
        n_iteration.data = [];
        return n_iteration;
    }
    function getStepProto(step) {
        var n_step = {};
        n_step.type = step.type;
        n_step.name = step.name;
        if (step.type === level.reusable) {
            n_step.status = {};
        }
        n_step.data = [];
        return n_step;
    }
    function getStepDataProto(step) {
        if (step.type === level.reusable) {
            return [];//reusable data is Array type
        }
        var stepData = {};
        stepData[ID.STEP.no] = step.data[ID.STEP.no];
        stepData[ ID.STEP.action] = step.data[ ID.STEP.action];
        stepData[ ID.STEP.desc] = step.name;
        stepData[ ID.STEP.res] = {};
        return stepData;
    }
    function getResult(step) {
        var res = {};
        res[ID.STEP.status] = step.data[ID.STEP.status];
        res[ID.STEP.time] = step.data[ ID.STEP.time];
        res[ID.STEP.link] = step.data[ID.STEP.link];
        res[ID.STEP.objects] = step.data[ID.STEP.objects];
        res[ID.STEP.comparison] = step.data[ID.STEP.comparison];
        res[ID.STEP.actual] = step.data[ID.STEP.actual];
        res[ID.STEP.expected] = step.data[ID.STEP.expected];
        return res;
    }
    function addStep(view, step, stepIndex, i_index) {
        if (!STEPS[i_index].data[stepIndex].data[ID.STEP.no]) {
            STEPS[i_index].data[stepIndex].data = getStepDataProto(step);
        }
        (STEPS[i_index].data[stepIndex].data[ID.STEP.res])[view] = getResult(step);
    }
    function addRStep(view, step, i_index, reusableIndex) {
        var xdata = STEPS[i_index];
        var exist = true, lindex;
        reusableIndex.forEach(function(index) {
            //travel to the the leaf instance
            if (xdata.data.length > 0 && xdata.data[index])
                xdata = xdata.data[index];
            else {
                exist = false;
                lindex = index;
            }
        });
        if (!exist) {//create new data if not there
            xdata.data[lindex] = getStepProto(step);
            xdata = xdata.data[lindex];
            xdata.data = getStepDataProto(step);
        }
        //update the results if it is a step
        if (xdata.type === level.step)
            (xdata.data[ID.STEP.res])[view] = getResult(step);
        return xdata;
    }

    function addReusable(view, reusable, i_index, reusableIndex) {
        var tmpindexes;
        reusable.data.forEach(function(step, stepIndex) {
            tmpindexes = reusableIndex.slice(0);//geting the copy(clone array) of leaf path.. you dont wanna mess with existing object
            tmpindexes.push(stepIndex);//adding the new level to the path
            if (step.type === level.reusable) {
                var innerReuse = addRStep(view, step, i_index, tmpindexes);//create a reusable type step
                innerReuse.status[view] = step.status || ""; // add reusable level result
                addReusable(view, step, i_index, tmpindexes); //call to update the reusable's steps
            } else
                addRStep(view, step, i_index, tmpindexes); //add reusable steps
        });
    }
    function addIteration(view, iteration, i_index) {
        if (!STEPS[i_index]) {
            STEPS[i_index] = getIterationProto(iteration);
        }
        STEPS[i_index].status[view] = iteration.status;
        iteration.data.forEach(function(step, stepIndex) {
            if (!STEPS[i_index].data[stepIndex])
            {
                STEPS[i_index].data[stepIndex] = getStepProto(step);
            }
            if (step.type === level.step)
            {
                addStep(view, step, stepIndex, i_index);
            } else if (step.type === level.reusable) {
                STEPS[i_index].data[stepIndex].status[view] = step.status || "";
                addReusable(view, step, i_index, [stepIndex]);
            }
        });
    }
    function addView(view) {
        data[view].STEPS.forEach(function(iteration, index) {
            addIteration(view, iteration, index);
        });
    }
    function getExeData() {
        browserHeaders.forEach(function(view) {
            addView(view);
        });
        return STEPS;
    }
    return getExeData();
}

function scrollToTop() {
    $(window).scroll(function() {
        if ($(this).scrollTop() > 100) {
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
            scope.setView(e.target.selectedOptions[0].label);
        });
    };
    sel.select2();
    sel.on("change", setv);

};
$(document).ready(function() {
    scrollToTop();
    initGalenReport();
    browserHeaders.forEach(function(browser) {
        setSNGLExeTab((browser).escape());
    });
    setGRPExeTab();
    DropDown();
});
function initGalenReport()
{
    $(document).keydown(function(e) {
        if (e.keyCode === 27) {
            hidePopup();
        }
    });
    $('a.exe.table.FAIL').click(setStatus);
    $('a.exe.table.PASS').click(setStatus);
    $('a.exe.table.SCREENSHOT').click(setStatus);

    $(".popup-close-link").click(onPopupCloseClick);
    $("#screen-shadow").click(function() {
        hidePopup();
    });
    $('div.image-comparison').hide();
    $('div.screenshot-canvas').hide();
}