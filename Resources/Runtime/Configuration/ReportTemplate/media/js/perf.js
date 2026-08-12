

//<editor-fold defaultstate="collapsed" desc="lib">
var $ = $ || {};
Math.round0 = function (x) {
    return Math.max(0, Math.round(x));
};
Math.round1 = function (x) {
    return Math.max(1, Math.round(x));
};
$.shortLink = function (url, l) {
    var limit = l || 60;
    if (url.length < limit) {
        return url;
    } else {
        var trimParams = url.split("?")[0];
        if (trimParams.length < limit - 3) {
            return trimParams + '...';
        } else {
            return trimParams.substring(0, limit - 10)
                    + "..." + trimParams.substring(trimParams.length - 10);
        }
    }

};
$.pie = {
    tip: function () {
        var tip;
        var getTip = function () {
            if (!tip) {
                tip = $('#pieTip');
            }
            return tip;
        };
        return getTip;
    }(),
    onUpdate: function (pie) {
        var oldRdn = 4.68;
        pie.data.forEach(function (entry, i) {
            var pieRdn = ((Math.max((entry.time === pie.total ? -1 : 0) + entry.time, 0) / pie.total) * (Math.PI * 2)),
                    endRdn = oldRdn + pieRdn;
            pie.pies[i][0].setAttribute("d", [
                //M => move to (x,y) => outer edge point on circle(start of current or end of old pie sect.) 
                'M', pie.center.X + Math.cos(oldRdn) * pie.rad, pie.center.Y + Math.sin(oldRdn) * pie.rad,
                //A => rx ry x-axis-rotation large-arc-flag sweep-flag x y => rx,ry radius(same for circle)
                //    x-axis-rotation => deg. - 0 (does'n mater in this case (circle))
                //    large-arc-flag  => (0/1) =>  if current timings covers morethan 50% of pie draw large arc ->1 else ->0 
                //    sweep-flag =>  (0/1) +ve angle or -ve angle => select the inner or outer circle 
                'A', pie.rad, pie.rad, 0, (entry.time * 2 > pie.total ? 1 : 0), 1, pie.center.X + Math.cos(endRdn) * pie.rad, pie.center.Y + Math.sin(endRdn) * pie.rad,
                //L => line to (x,y) => connects the current pie sect. end on outer edge of circle to the pie center
                'L', pie.center.X, pie.center.Y,
                //Z => close path => joins the starting position in this case M
                'Z'
            ].join(' '));
            oldRdn += pieRdn;
        });
    },
    onLeave: function (e) {
        e.data.tip().hide();
    },
    onEnter: function (e) {
        var pie = e.data;
        pie.tip().css({
            top: e.pageY - 40,
            left: e.pageX - pie.tip().width() / 2
        });
        var index = $(this).data().order;
        pie.tip().text(pie.data[index].name + "  " + pie.data[index].time + " ms").fadeIn(250);
    },
    newPie: function (op) {
        var sum = 0;
        function toData(timings, data) {
            var d = data || [];
            timings.forEach(function (e) {
                data.push({name: e.name, time: 10, key: e.key});
                sum += 10;
            });
            return d;
        }
        function onClear() {
            for (var o in this) {
                this[o] = undefined;
            }
        }
        return {
            clear: onClear,
            onUpdate: $.pie.onUpdate, onEnter: $.pie.onEnter, onLeave: $.pie.onLeave,
            tip: $.pie.tip,
            center: {X: op.centerX, Y: op.centerY}, rad: op.radius,
            groups: [], pies: [], data: toData(op.timings, []),
            total: sum
        };
    }
};
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="init">

var perf = {
    pSpeed: {},
    har: [],
    pageTimings: [{"key": "onContentLoad",
            "name": "Content Load"
        }, {"key": "onLoad",
            "name": "Page Load"
        }],
    timings: [
        {"key": "blocked",
            "name": "Blocking"
        }, {"key": "dns",
            "name": "DNS"
        }, {"key": "ssl",
            "name": "SSL/TLS"
        }, {"key": "connect",
            "name": "Connecting"
        }, {"key": "send",
            "name": "Sending"
        }, {"key": "wait",
            "name": "Waiting"
        }, {"key": "receive",
            "name": "Receiving"
        }
    ]
};

window.svgUrl = 'http://www.w3.org/2000/svg';
window.timingsFPattern = /^(|[>|<]?=?(-1|0|\d{1,6})|(-1|0|\d{1,6})\-(0|\d{1,6}))$/;
window.piePopup = $.pie.newPie({centerX: 60, centerY: 60, radius: 60, timings: perf.timings});
var Params;
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

function onPageSpeed(res) {
    if (res)
        if (res.logs) {
            perf.pSpeed.data.push(res);
        } else
            res.forEach(function (e) {
                perf.pSpeed.data.push(e);
            });
}
function onHar(data) {
    var har = data;
    var pMap = {};
    har.log.pages.forEach(function (page) {
        pMap[page.id] = page;
        page.entries = [];
    });
    har.log.entries.forEach(function (entry) {
        pMap[entry.pageref].entries.push(entry);
    });
}
function onPerfLog(log) {
    var keys;
    if (Params.SC && Params.TC) {
        keys = [Params.SC + '_' + Params.TC];
    } else {
        keys = Object.keys(log);
    }
    perf.pSpeed.data = [];
    keys.forEach(function (key) {
        if (!Params.SC || new RegExp('^' + Params.SC).test(key)) {
            onPageSpeed(log[key].pSpeed);
            log[key].har.forEach(function (entry) {
                onHar(entry.har);
                perf.har.push(entry);
            });
        }
    });
}
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="controllers">

var app = angular.module('report', []);

app.service('current', function () {
    this.expandAll = false;
});
//<editor-fold defaultstate="collapsed" desc="har">
app.controller('har', ['$scope', 'current', '$timeout', function ($scope, current, $timeout) {
        $scope.timings = perf.timings;
        $scope.Math = window.Math;
        $scope.har = perf.har;
        $scope.w = 720;
        $scope.p = {};
        $scope.pattern = window.timingsFPattern;
        $scope.resolvedLink = function (req) {
            var asset = req.url.split("?")[0].split("/").pop();
            return $.shortLink(((asset === "" ? req.url.split("//").pop() : asset)), 24);
        };
        $scope.shortLink = function (url, l) {
            return $.shortLink(url, l);
        };

        $scope.initPage = function (page) {
            page._start = new Date(page.startedDateTime);
            page.entries.sort(function (a, b) {
                return  new Date(a.startedDateTime) < new Date(b.startedDateTime) ? -1 : 1;
            });
            page._loadTime = page.pageTimings.onLoad;
            page.entries.map(function (e) {
                e.startedTime = new Date(e.startedDateTime) - page._start;
                page._loadTime = Math.max(page._loadTime, e.startedTime + e.time);
            });
            page._ratio = $scope.w / page._loadTime;
            page.pageTimings._contentLoad = Math.round1(page.pageTimings.onContentLoad * page._ratio);
            page.pageTimings._pageLoad = Math.round1(page.pageTimings.onLoad * page._ratio);
            page.entries.map(function (e) {
                initX(page, e);
            });
        };
        function initX(page, e) {
            if (!e.timings._x) {
                e.timings._x = {"next": e.startedTime};
                if (e.timings._x.next > 0)
                    e.timings._x.next = Math.round1(e.timings._x.next * page._ratio);
                e._width = {};
                $scope.timings.map(function (t) {
                    e._width[t.key] = getWidth(page, e, t.key);
                });
            }
            function getWidth(page, e, k) {
                var rStart = e.timings._x.next;
                e.timings._x[k] = rStart;
                var width = Math.max(0, e.timings[k]);
                if (width > 0) {
                    width = Math.round1(width * page._ratio);
                }
                e.timings._x.next = rStart + width;
                return width;
            }
        }

        $scope.getX = function (page, x) {
            return x;
        };
        $scope.onClickResource = function (entry, page, event) {
            $scope.p = entry;
            $scope.p.evt = {
                onLoad: signed(page.pageTimings.onContentLoad - $scope.p.startedTime),
                onContentLoad: signed(page.pageTimings.onLoad - $scope.p.startedTime)
            };
            updatePopup(window.piePopup, entry.timings, entry.time);
            showPopup({left: event.pageX, top: event.pageY - 23});
        };
        $scope.drawDnPT = function (page, elem) {
            var dnChart = $.pie.newPie({centerX: 60, centerY: 60, radius: 60, timings: perf.timings});
            $scope.drawDn(dnChart, $(elem), page.entries[0]["timings"], page.entries[0]["timings"].time);
        };
        $scope.drawDnLT = function (page, elem) {
            var dnChart = $.pie.newPie({centerX: 60, centerY: 60, radius: 60, timings: perf.pageTimings});
            $scope.drawDn(dnChart, $(elem), page.pageTimings);
        };
        $scope.drawDn = function (dnChart, $pathGroup, entry, sum) {
            dnChart.data.forEach(function (data, i) {
                var g = document.createElementNS(window.svgUrl, 'g');
                g.setAttribute("data-order", i);
                var p = document.createElementNS(window.svgUrl, 'path');
                p.setAttribute("class", data.key);
                var $grp = $(g).appendTo($pathGroup);
                dnChart.groups.push($grp);
                $grp.on("mouseenter", dnChart, dnChart.onEnter);
                dnChart.pies.push($(p).appendTo($grp));
            });
            $pathGroup.on("mouseleave", dnChart, dnChart.onLeave);
            updatePopup(dnChart, entry, sum);
        };
        $scope.initDataTable = function (ele) {
            $(ele).dataTable({
                "bSortCellsTop": true,
                orderCellsTop: true,
                "scrollX": true,
                "paging": false,
                "scrollY": "340px",
                "scrollCollapse": true,
                "bInfo": false
            });
        };
        $scope.q = initQ();
        $scope.filterTimings = function (page) {
            var range;
            $scope.q.cols.forEach(function (key) {
                range = $scope.q[key][page._id];
                if (range)
                    range.fnInit(range);
            });

            page.entries.forEach(function (e) {

                for (var c = 0; c < $scope.q.cols.length; c++) {
                    var col = $scope.q.cols[c];
                    var data;
                    if ($scope.q.index[col] === 0) {
                        data = e.request.url;
                    } else
                    if ($scope.q.index[col] < 8) {
                        data = e.timings[col];
                    } else if ($scope.q.index[col] === 8) {
                        data = e.time;
                    } else {
                        data = e.startedDateTime;
                    }
                    var r = $scope.q[col][page._id];
                    if (r && r.eq) {
                        try {
                            e._accept = (eval(r.eq));
                        } catch (e) {
                            console.error(e);
                        }
                        if (!e._accept)
                            break;
                    }
                }
            });
        };
        function initQ() {
            var index = {url: 0, total: 8, time: 9};
            var cols = ['url'];
            var q = {url: {}, total: {}, time: {}, index: index, cols: cols};
            perf.timings.forEach(function (e, i) {
                q[e.key] = {};
                index[e.key] = i + 1;
                cols.push(e.key);
            });
            cols.push('total');
            cols.push('time');
            return q;
        }
        function signed(v) {
            return v > 0 ? "+" + v : v;
        }

        function showPopup(loc) {
            var ele = angular.element(document.getElementsByClassName('popover')[0]);
            var theHeight = 130;
            $(".popover").removeClass("mod_popover");
            $(".popover").removeClass("mod_popover_top");
            if ((window.innerWidth - loc.left) < 302) {
                $(".popover").addClass("mod_popover");
                ele.css({'left': loc.left - 216 + 'px', 'top': (loc.top - theHeight - 58) + 'px'});
            } else if ((window.innerHeight - loc.top) < theHeight) {
                $(".popover").addClass("mod_popover_top");
                ele.css({'left': loc.left - 138 + 'px', 'top': (loc.top - theHeight - 58) + 'px'});
            } else {
                $(".popover").removeClass("mod_popover");
                $(".popover").removeClass("mod_popover_top");
                ele.css({'left': (loc.left + 25) + 'px', 'top': (loc.top - (theHeight / 2) - 10) + 'px'});
            }
            $('#popover').fadeIn(500);
        }
        function updatePopup(chart, items, total) {
            var sum = 0;
            chart.data.forEach(function (d) {
                d.time = items[d.key];
                sum += d.time;
            });
            chart.total = total || sum;
            chart.onUpdate(chart);
        }
        $scope.filterRow = function (e) {
            return e._accept === undefined || e._accept;
        };
        $scope.$watch(function () {
            return current.expandAll;
        }, function (n, o) {
            if (!n)
                for (var i in $scope.har) {
                    for (var pi in $scope.har[i].har.log.pages) {
                        $scope.har[i].har.log.pages[pi]._show = n;
                    }
                }
            else {
                $timeout(expandVisible, 300, false);
            }
        });
    }]);
//</editor-fold>

app.controller('pageSpeed', ['$scope', 'current','$sce', function ($scope, current,$sce) {
        $scope.data = [];
        $scope.init = function () {
            $scope.data = perf.pSpeed.data;
        };
        $scope.trust = function (code) {
            return $sce.trustAsHtml(code);
        };
        $scope.getColor = function (value) {
            if (value < 0) {
                return 'gray';
            } else {
                return ["hsl(", 1.20 * (value), ",55%,50%)"].join("");
            }
        };
        $scope.shortLn = function (url) {
            return $.shortLink(url, 130);
        };
        $scope.$watch(function () {
            return current.expandAll;
        }, function (n, o) {
            $scope.data.map(function (page, i) {
                page._show = n;
                page.logs.map(function (log) {
                    log._show = n;
                });
            });
        });
    }]);

app.controller('main', ['$scope', 'current', function ($scope, current) {
        $scope.psdata = [];
        $scope.cView = 'chart';
        $scope.status = "Expand";
        $scope.expandOrcollapse = function () {
            current.expandAll ? ($scope.status = "Expand") : ($scope.status = "Collapse");
            current.expandAll = !current.expandAll;
        };
        $scope.isExpand = function () {
            return current.expandAll;
        };
    }]);
//<editor-fold defaultstate="collapsed" desc="directives-element">
$.regex = $.regex || {};
$.regex.search = {all: /^(|[>|<]?=?(-1|0|\d{1,6})|(-1|0|\d{1,6})\-(0|\d{1,6}))$/,
    between: /^(-1|0|\d{1,6})\-(0|\d{1,6})$/,
    gt_lt: /^[>|<]=?(-1|0|\d{1,6})$/
};
app.directive('filterText', function () {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            var col = attr.name;
            $scope.q[col][$scope.page._id] = {};
            $scope.q[col][$scope.page._id].fnInit = function (r) {
                if (r.v === '' || r.v === undefined) {
                    r.eq = 'true';
                } else
                    r.eq = "data.indexOf('" + r.v + "') !== -1";
            };
            function updateModel(v) {
                $scope.q[col][$scope.page._id].v = v;
            }
            ele.bind('change', function (e) {
                updateModel(ele[0].value);
            });
            ele.bind("keydown", function (e) {
                if (e.keyCode === 13/*enter*/) {
                    updateModel(ele[0].value);
                    $scope.$apply(function () {
                        $scope.filterTimings($scope.page);
                    });
                    $(window).resize();
                } else if (e.keyCode === 27/*escape*/) {
                    updateModel(ele[0].value = '');
                }
            });
        }
    };
});
app.directive('filterTimings', function () {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            var col = attr.name;
            attr.$set('title', 'Ex. >10,=10,<=10,1-10 ');
            $scope.q[col][$scope.page._id] = {};
            $scope.q[col][$scope.page._id].fnInit = function (r) {
                function validInput(v) {
                    return $.regex.search.all.test(v);
                }
                if (validInput(r.v)) {
                    if (r.v === undefined || r.v === '') {
                        r.eq = 'true';
                    } else if ($.regex.search.gt_lt.test(r.v)) {
                        r.eq = 'data' + r.v;
                    } else {
                        if ($.regex.search.between.test(r.v)) {
                            var sep = r.v.lastIndexOf('-');
                            var x = r.v.substring(0, sep), y = r.v.substring(sep + 1);
                            r.eq = 'data >= ' + Math.min(x, y) + ' && data <= ' + Math.max(x, y);
                        } else {
                            r.eq = 'data ==' + r.v;
                        }
                    }
                } else {
                    r.eq = false;
                }
            };
            function updateModel(v) {
                $scope.q[col][$scope.page._id].v = v;
            }
            ele.bind('change', function (e) {
                updateModel(ele[0].value);
            });
            ele.bind("keydown", function (e) {
                if (e.keyCode === 13/*enter*/) {
                    updateModel(ele[0].value);
                    $scope.$apply(function () {
                        $scope.filterTimings($scope.page);
                    });
                    $(window).resize();
                } else if (e.keyCode === 27/*escape*/) {
                    updateModel(ele[0].value = '');
                }
            });
        }
    };
});
app.directive('elemReady', function () {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            ele.ready(function () {
                $scope.$apply(function () {
                    var e = $scope.$eval(attr.elemReady);
                    if (e.data)
                        $scope[e.callback]($scope[e.data], ele);
                    else
                        $scope[e.callback](ele);
                });
            });
        }
    };
});
app.directive('svgDef', function ($compile) {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            if (!attr.xmlns) {
                var a = $scope.$eval(attr.svgDef);
                attr.$set('width', a[2] + '');
                attr.$set('height', a[3] + '');
                attr.$set('xmlns', 'http://www.w3.org/2000/svg');
                attr.$set('xmlns:xlink', 'http://www.w3.org/1999/xlink');
                attr.$set('viewBox', a.join(' ') + '');
                attr.$set('svg-def', null);
                $compile(ele)($scope);
            }
        }
    };
});
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="directives-template">
app.directive('linkPsData', function () {
    return {
        template: '<a title="{{::data.comments}}" style="word-wrap: break-word;" href="{{::data.url}}" target = "_blank"><span title="{{::data.url}}">{{::shortLn(data.url)}}</span></a>'
    };
});
app.directive('linkPsPageTitle', function () {
    return {
        template: '<label class="score" ng-if="page.score>-1" ng-attr-style="background-color:{{::page._color}};">' +
                '{{::page.score}}</label> <label title="{{::page.name}}" style = "max-width: 1000px;text-overflow: ellipsis;white-space: nowrap;font-size: 16px;font-weight: 300;" class="title {{::page._show}}">{{::page.name}}</label>'
    };
});
app.directive('linkPsLogTitle', function () {
    return {
        template: "<label class='content score' ng-attr-style='background-color:{{::log._color}};'>" +
                "{{::log.score<0?'NA':log.score}}</label><label style = \"font-size: 14px;margin-left: 10px;\" class='content title'>{{::log.name}}</label>"
    };
});
//</editor-fold>

window.piePopup.init = function ($pathGroup) {
    window.piePopup.data.forEach(function (data, i) {
        var g = document.createElementNS(window.svgUrl, 'g');
        g.setAttribute("data-order", i);
        g.setAttribute("data-bar", "true");
        var p = document.createElementNS(window.svgUrl, 'path');
        p.setAttribute("class", data.key);
        p.setAttribute("data-bar", "true");
        var $grp = $(g).appendTo($pathGroup);
        window.piePopup.groups.push($grp);
        $grp.on("mouseenter", window.piePopup, window.piePopup.onEnter);
        window.piePopup.pies.push($(p).appendTo($grp));
    });
    $pathGroup.on("mouseleave", window.piePopup, window.piePopup.onLeave);
};
//</editor-fold>

//JS of main file
$(document).ready(function () {
    $(".dataTables_scrollHeadInner .report_table").css("width", "auto");
    $('.dataTables_filter').hide();
    $("a[href='#pageSpeed']").click(function () {
        $("div[ng-controller=har]").hide();
        $(".pageSpeed").fadeIn();
    });
    $("a[href='#chart'], a[href='#table']").click(function () {
        $(".pageSpeed").hide();
        $("div[ng-controller=har]").fadeIn();
    });

    $('.scrollHar').on("scroll", function (e) {
        var scope = angular.element($('.panel.panel-primary.perContainer')).scope();
        if (scope.isExpand()) {
            expandVisible();
        }
    });

});
function expandVisible() {
    var vis;
    if ($('.scrollHar:in-viewport').length > 0) {
        //if table / chart visible expand the pages in view port
        vis = $('.hideOrShow:in-viewport');
    } else {
        //if table / chart not visible(inview port) expand first 2 pages 
        vis = $('.hideOrShow');
    }
    if (vis.length > 2) {
        vis = vis.slice(0, Math.min(2, vis.length - 1));
    }
    vis.each(function (i, e) {
        if (!e.nextElementSibling)
            e.click();
    });
}