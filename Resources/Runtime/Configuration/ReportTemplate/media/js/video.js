/*Added by: Chester Luzon 02/16/2024*/
var isTCMatched = function(exe) {
    return (exe[ID.scname] === Params.SC) && (exe[ID.name] === Params.TC);
};

function getBroUID(row, sep) {
    return row[ID.browser] + sep + row[ID.bversion] + sep + row[ID.platform] + sep + row[ID.iType];
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
var contains = function(array, val) {
    return array.indexOf(val) !== -1;
};

var ID = {"scname": "scenarioName", "name": "testcaseName", "stime": "startTime", "etime": "endTime",
    "stat": "status", "exetime": "exeTime", "browser": "browser", "iterations": "iterations",
    "bversion": "bversion", "platform": "platform", "iType": "iterationType", "emode": "runConfiguration",
    "STEP": {"no": "stepno", "name": "stepName", "action": "action", "desc": "description", "status": "status", "time": "tStamp", "res": "result", "link": "link", "objects": "objects", "actual": "actual", "expected": "expected", "comparison": "comparison"}};
var Params;
var broID;

(window.onpopstate = function () {
    var match,
            pl = /\+/g,
            search = /([^&=]+)=?([^&]*)/g,
            decode = function (s) {
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

var app = angular.module('videoReport', []);

app.controller('videoCtrl', ['$scope', function ($scope) {
         $scope.Details = getExeData();
         $scope.view = broID;
         $scope.steps = $scope.Details[$scope.view].STEPS;
    }]);


$.regex = $.regex || {};
$.regex.search = {all: /^(|[>|<]?=?(-1|0|\d{1,6})|(-1|0|\d{1,6})\-(0|\d{1,6}))$/,
    between: /^(-1|0|\d{1,6})\-(0|\d{1,6})$/,
    gt_lt: /^[>|<]=?(-1|0|\d{1,6})$/
};

var getExeData = function() {
    var det = {};
    var eList = DATA.EXECUTIONS;
    try {
        eList.forEach(function(exe) {
            if (isTCMatched(exe)) {
                broID = getBroUID(exe, " ");
                det[broID] = exe;
            }
        });
    } catch (ex) {
    }
    return det;
};