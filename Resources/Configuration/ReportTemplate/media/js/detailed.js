
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

// ===== NEW v2 REPORT RENDERING FUNCTIONS =====
// Resolve screenshot paths to correct relative paths
function resolveScreenshotPath(link) {
    if (!link) return '';
    if (link.startsWith('data:') || link.startsWith('http://') || link.startsWith('https://')) {
        return link;
    }
    // Screenshots are in ./img/ relative to the HTML file
    // Link format: /img/filename.png or ./img/filename.png
    if (link.startsWith('/img/')) {
        return '.' + link; // Convert /img/ to ./img/
    }
    return link;
}

// Global search state
var globalStepFilter = '';

// Expand all steps and reusables
function expandAllSteps() {
    document.querySelectorAll('[data-step-body], [data-reusable-body]').forEach(function(element) {
        element.style.display = 'block';
        const arrow = element.parentElement.querySelector('.step-arrow, .reusable-arrow');
        if (arrow) arrow.style.transform = 'rotate(180deg)';
    });
}

// Collapse all steps and reusables
function collapseAllSteps() {
    document.querySelectorAll('[data-step-body], [data-reusable-body]').forEach(function(element) {
        element.style.display = 'none';
        const arrow = element.parentElement.querySelector('.step-arrow, .reusable-arrow');
        if (arrow) arrow.style.transform = 'rotate(0deg)';
    });
}

// Modern recursive renderer for detailed-v2.html
function renderStepsV2(iterations, showFailedOnly = false, stepFilter = '') {
    function escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe.toString()
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }
    
    function matchesFilter(name, description) {
        if (!stepFilter || stepFilter.trim() === '') return true;
        const filter = stepFilter.toLowerCase().trim();
        return (name || '').toLowerCase().includes(filter) || (description || '').toLowerCase().includes(filter);
    }
    
    function renderStep(step, keyPath) {
        const data = step.data || {};
        const status = (data.status || '').toLowerCase();
        if (showFailedOnly && status !== 'fail') return '';
        if (!matchesFilter(data.stepName || data.action || step.name, data.description)) return '';
        
        const action = (data.action || '').toLowerCase();
        let statusIcon = '';
        
        // Check status first for proper icon color
        if (status === 'fail') {
            statusIcon = '<svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/></svg>';
        } else if (status === 'pass' || status === 'done') {
            statusIcon = '<svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>';
        } else {
            statusIcon = '<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>';
        }
        
        return `<div class="step-item" data-key="${keyPath}"><div class="step-item__marker step-item__marker--${status}">${statusIcon}</div><div class="step-item__content"><div class="step-item__header cursor-pointer" onclick="toggleStepV2('${keyPath}')"><div class="flex-1"><div class="flex items-center gap-3 mb-1"><span class="text-sm font-mono text-muted">#${escapeHtml(data.stepno || '')}</span><span class="p-3 rounded-lg text-sm font-mono whitespace-pre-wrap bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">${escapeHtml(data.stepName || data.action || step.name || 'Step')}</span></div>${data.description ? `<div class="text-sm text-gray-600 dark:text-gray-400 ml-12">${escapeHtml(data.description)}</div>` : ''}</div><div class="flex items-center gap-3"><span class="text-xs text-muted">${escapeHtml(data.tStamp || '')}</span><span class="badge badge--${status}">${escapeHtml(data.status || '')}</span><svg class="w-5 h-5 text-gray-400 transition-transform duration-200 step-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg></div></div><div class="step-item__body" style="display: none;" data-step-body="${keyPath}">${data.action ? `<div class="mb-3"><strong class="text-sm">Action:</strong> <span class="text-sm">${escapeHtml(data.action)}</span></div>` : ''}${data.result ? `<div class="mb-3"><strong class="text-sm">Result:</strong> <span class="text-sm">${escapeHtml(data.result)}</span></div>` : ''}${data.failureMsg || data.failureMessage ? `<div class="mb-3 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded"><strong class="text-sm text-red-700 dark:text-red-400">Failure Message:</strong> <pre class="text-xs text-red-600 dark:text-red-300 mt-1 whitespace-pre-wrap">${escapeHtml(data.failureMsg || data.failureMessage)}</pre></div>` : ''}${data.objects ? `<div class="mb-3"><strong class="text-sm">Objects:</strong> <span class="text-sm">${escapeHtml(data.objects)}</span></div>` : ''}${data.actual ? `<div class="mb-3"><strong class="text-sm">Actual:</strong> <pre class="text-xs bg-gray-100 dark:bg-gray-800 p-2 rounded overflow-x-auto">${escapeHtml(data.actual)}</pre></div>` : ''}${data.expected ? `<div class="mb-3"><strong class="text-sm">Expected:</strong> <pre class="text-xs bg-gray-100 dark:bg-gray-800 p-2 rounded overflow-x-auto">${escapeHtml(data.expected)}</pre></div>` : ''}${data.link ? `<div class="mb-3"><strong class="text-sm">Screenshot:</strong><br><img src="${resolveScreenshotPath(data.link)}" alt="Step Screenshot" class="mt-2 rounded border border-gray-200 dark:border-gray-700 max-w-full cursor-pointer" onerror="this.style.display='none'" onclick="window.open('${resolveScreenshotPath(data.link)}', '_blank')"></div>` : ''}</div></div></div>`;
    }
    
    function renderReusable(reusable, keyPath) {
        const status = (reusable.status || '').toLowerCase();
        const statusIcon = status === 'pass' ? '<svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>' : '<svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/></svg>';
        let html = `<div class="reusable-component rounded-lg overflow-visible ${status === 'pass' ? 'reusable-header--passed' : 'reusable-header--failed'}" data-key="${keyPath}"><div class="flex items-center gap-3 p-4 cursor-pointer bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors" onclick="toggleReusableV2('${keyPath}')"><div class="flex items-center justify-center flex-shrink-0">${statusIcon}</div><div class="flex-1"><div class="font-semibold text-gray-800 dark:text-gray-200">${escapeHtml(reusable.name)}</div>${reusable.description ? `<div class="text-sm text-gray-600 dark:text-gray-400">${escapeHtml(reusable.description)}</div>` : ''}</div><div class="flex items-center gap-2"><span class="badge text-xs badge--${status}">${escapeHtml(reusable.status || '')}</span><svg class="w-5 h-5 text-gray-400 transition-transform duration-200 reusable-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg></div></div><div class="border-t border-gray-200 dark:border-gray-700 p-4 bg-gray-50 dark:bg-gray-900" style="display: none;" data-reusable-body="${keyPath}"><div class="step-timeline">`;
        (reusable.data || []).forEach((child, idx) => {
            if (child.type === 'step') html += renderStep(child, keyPath + '-' + idx);
            else if (child.type === 'reusable') html += renderReusable(child, keyPath + '-' + idx);
        });
        return html + '</div></div></div>';
    }
    
    let html = '';
    (iterations || []).forEach((item, idx) => {
        const keyPath = '' + idx;
        if (item.type === 'step') html += renderStep(item, keyPath);
        else if (item.type === 'reusable') html += renderReusable(item, keyPath);
    });
    return html;
}

// Toggle step expansion
function toggleStepV2(keyPath) {
    const body = document.querySelector('[data-step-body="' + keyPath + '"]');
    const arrow = document.querySelector('[data-key="' + keyPath + '"] .step-arrow');
    if (body) {
        if (body.style.display === 'none') {
            body.style.display = 'block';
            if (arrow) arrow.style.transform = 'rotate(180deg)';
        } else {
            body.style.display = 'none';
            if (arrow) arrow.style.transform = 'rotate(0deg)';
        }
    }
}

// Toggle reusable expansion
function toggleReusableV2(keyPath) {
    const body = document.querySelector('[data-reusable-body="' + keyPath + '"]');
    const arrow = document.querySelector('[data-key="' + keyPath + '"] .reusable-arrow');
    if (body) {
        if (body.style.display === 'none') {
            body.style.display = 'block';
            if (arrow) arrow.style.transform = 'rotate(180deg)';
        } else {
            body.style.display = 'none';
            if (arrow) arrow.style.transform = 'rotate(0deg)';
        }
    }
}

// Inject rendered steps into #steps-container
function injectStepsV2(showFailedOnly = false, stepFilter = '') {
    if (!window.DATA || !window.DATA.EXECUTIONS) return;
    let html = '';
    window.DATA.EXECUTIONS.forEach(function(exe, exeIdx) {
        if (exe.STEPS) {
            exe.STEPS.forEach(function(iteration, iterIdx) {
                if (showFailedOnly && iteration.status !== 'FAIL') return;
                html += `<div class="mb-6 pb-6 border-b border-gray-200 dark:border-gray-700 last:border-b-0"><div class="flex items-center gap-3 mb-4"><div class="px-3 py-1 rounded-full text-sm font-semibold ${iteration.status === 'PASS' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}">${iteration.name || 'Iteration'}</div><span class="text-sm text-muted">${iteration.status || ''}</span></div><div class="step-timeline">${renderStepsV2(iteration.data, showFailedOnly, stepFilter)}</div></div>`;
            });
        }
    });
    const container = document.getElementById('steps-container');
    if (container) container.innerHTML = html;
}

// ===== END NEW v2 FUNCTIONS =====


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
    // Guard for Angular - skip if not loaded
    if (typeof angular === 'undefined') {
        console.log('Angular not loaded, skipping controller initialization');
        return;
    }

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
    if (typeof $ === 'undefined' || typeof angular === 'undefined') {
        console.log('jQuery or Angular not loaded, skipping dropdown initialization');
        return;
    }
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

// Guard jQuery initialization
if (typeof jQuery !== 'undefined' && typeof $ !== 'undefined') {
    $(document).ready(function() {
        scrollToTop();
        initGalenReport();
        browserHeaders.forEach(function(browser) {
            setSNGLExeTab((browser).escape());
        });
        setGRPExeTab();
        DropDown();
    });
} else {
    console.log('jQuery not loaded, skipping $(document).ready initialization');
}
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