
var perf = {
    har: [],
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
var App = {data: {}, harClient: harClient, project: {}};
App.log = function () {
    console.log.apply(console, arguments);
};
//<editor-fold defaultstate="collapsed" desc="angular modules">

var app = angular.module('report', []);

//<editor-fold defaultstate="collapsed" desc="service">
app.service('current', function () {

    //<editor-fold defaultstate="collapsed" desc="data">

    //<editor-fold defaultstate="collapsed" desc="const">
    this.cap = {
        bro: {
            IE: ['IE', 'Edge'],
            Firefox: 'Firefox',
            Safari: 'Safari',
            Chrome: 'Chrome',
            known: ['chrome', 'safari', 'ie', 'firefox']
        },
        platf: {
            WIN: ['WIN7', 'WIN8', 'WIN8.1', 'VISTA', 'XP'],
            MAC: ['MAC', 'SNOW_LEOPARD', 'MOUNTAIN_LION', 'MAVERICKS', 'YOSEMITE'],
            LINUX: ['LINUX', 'UNIX'],
            ANDROID: ['ANDROID'],
            IOS: ['IOS']
        }
    };
    this.cap.files = {
            ie: this.cap.bro.IE[0], edge: this.cap.bro.IE[1],
            firefox: this.cap.bro.Firefox, chrome: this.cap.bro.Chrome,
            safari:this.cap.bro.Safari, 'default': 'default'
    };
    this.harNames = ['raw', 'report', 'testcase', 'iteration', 'browser', 'version', 'platform', 'index', 'page'];
    this.resource = {
        "time": 0,
        "timings": {"send": 0, "connect": -1, "dns": -1, "ssl": -1, "blocked": 0, "wait": 0, "receive": 0},
        "request": {"url": ""},
        "startedDateTime": ""
    };
    this.views = {loading: -1, harCompare: 0, harSelector: 1, selectAsset: 2, selectRef: 3};
    //</editor-fold>

    this.searchView = false;
    this.data = {
        dataSet: [], refList: [], config: {ref: {}, selectedHars: []}
    };
    this.harsCompare = {};
    this.newAssets = [];
    this.assetsToAdd = [];
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="ops">
    this.getLinkName = function (req) {
        if (req) {
            var asset = req.url.split("?")[0].split("/").pop();
            return $.shortLink(((asset === "" ? req.url.split("//").pop() : asset)), 24);
        }
        return '';
    };
    this.getLinkLongName = function (req) {
        if (req) {
            var asset = new URL(req.url).pathname;
            return asset;
        }
        return '';
    };
    this.diffStr = function (l, r) {
        if (r === undefined)
            return;
        var diff = r - l;
        if (diff === 0)
            return;
        if (diff > 0)
            return '+' + diff;
        return diff;
    };
    this.sort = function (asc) {
        return {
            with : function (op) {
                return function (a, b) {
                    return (op.ref && a[op.ref]) ? -1 : (asc ? -1 : 1) *
                            (eval('a.' + op.key) > eval('b.' + op.key) ? -1 : 1);
                };
            }};
    };
    this.resolve = {har: function (har) {
            har.log.entries.remove(har.log.entries.find(function (entry) {
                return entry.request.url === 'document';
            }));
        },
        assetMap: function ($this, e, dest, _assets) {
            e[dest] = {};
            e.har.log.entries.map(function (entry) {
                var asset = $this.getLinkLongName(entry.request);
                e[dest][asset] = entry;
                if (!_assets[asset]) {
                    _assets[asset] = entry;
                }
            });
        }};
    this.init = function ($this) {
        return function ($scope) {
            if (App.harClient.connected) {
                App.harClient.getReportHistory({callback: onInitData});
            } else
                App.harClient.connect({
                    onErr: $.onConnError,
                    onServerErr: $.onServerError,
                    onOpen: function () {
                        App.harClient.getReportHistory({callback: onInitData});
                    }});

            function onInitData(d) {
                $scope.$apply(function () {
                    $this.data.dataSet = d.dataset;
                    $this.data.refList = d.refList;
                    if (onSettings(d.config))
                        $scope.updateView(true /*dont use apply*/);
                });
            }
            function onSettings(config) {
                if (!config || !config.ref) {
                    return true;
                }
                $this.data.config.ref = config.ref;
                App.harClient.getHarRefData({callback: onRefHarData, name: config.ref});
                function onRefHarData(d) {
                    $this.data.config.ref = d;
                    $this.data.config.ref.ref = true;
                    updateView();
                    function updateView() {
                        config.selectedHars = config.selectedHars || [];
                        $this.data.config.selectedHars = config.selectedHars;
                        if (config.selectedHars.length > 0)
                            $this.doCompare($this, $scope);
                        $scope.updateView();
                    }
                }
            }
        };
    };
    this.doCompare = function ($this, $scope) {
        App.harClient.getHarsData({
            callback: function onHarsData(data) {
                $scope.$apply($this.harsCompare.harsData = data.harsData);
                $scope.updateView();
            },
            selectedHars: $this.data.config.selectedHars
        });
    };
    //</editor-fold>

});
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="controllers">

//<editor-fold defaultstate="collapsed" desc="home">
app.controller('home', ['$scope', 'current', function ($scope, current) {
        $scope.refList;
        $scope.config;
        $scope.newAssets;
        $scope.views = current.views;
        $scope.updateView = function (no_need_for_apply) {
            if (no_need_for_apply) {
                update();
            } else
                $scope.$apply(update);
            function update() {
                $scope.homeView = (current.harsCompare.harsData && current.harsCompare.harsData.length > 0) ?
                        current.views.harCompare : current.views.selectHar;
            }
        };
        $scope.homeView = current.views.loading;
        $scope.if = function (view) {
            return $scope.homeView === view;
        };
        current.init(current)($scope);
        $scope.onSelectRef = function (ref) {
            current.data.config.ref = ref;
            App.harClient.getHarRefData({callback: onRefHarData, name: ref.name});
            function onRefHarData(d) {
                $scope.$apply(function () {
                    current.data.config.ref = d;
                    current.data.config.ref.ref = true;
                    $scope.homeView = current.views.harCompare;
                });
            }
        };
        $scope.resolvedLink = function (req) {           
            return current.getLinkName(req);
        };
        $scope.onAssetsSelect = function (select) {
            $scope.assetsToAdd = [];
            $scope.newAssets.map(function (asset) {
                if (select)
                    $scope.assetsToAdd.push(asset);
                $scope.cAssetsMap[asset]._sel = select;
            });
        };
        $scope.onAddtoRef = function () {

            if ($scope.assetsToAdd && $scope.assetsToAdd.length > 0) {
                current.assetsToAdd = $scope.assetsToAdd;
            }
            $scope.homeView = current.views.harCompare;
        };

        $scope.onSelectAsset = function (asset, stat) {
            if (stat)
                $scope.assetsToAdd.push(asset);
            else
                $scope.assetsToAdd.remove(asset);
        };

        $scope.$watch(function () {
            return current.newAssets;
        }, function (n, o) {
            if (n) {
                $scope.newAssets = n;
                $scope.assetsToAdd = [];
                $scope.cAssetsMap = current.cAssetsMap;
            }
        }, true);
        $scope.$watch(function () {
            return current.data.refList;
        }, function (n, o) {
            if (n) {
                $scope.refList = n;
            }
        }, true);
        $scope.$watch(function () {
            return current.data.config;
        }, function (n, o) {
            if (n) {
                $scope.config = n;
            }
        }, true);
    }]);
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="harSelector">
app.controller('harSelecter', ['$scope', 'current', '$timeout', function ($scope, current, $timeout) {

        $scope.dataSet = current.data.dataSet;
        $scope.selected = [];
        $scope.fltr = current.cap;
        $scope.$watch(function () {
            return current.data.dataSet;
        }, function (n, o) {
            if (n) {
                $scope.onReportHistory(n);
            }
        }, true);
        $scope.onReportHistory = function (dataset) {
            $scope.dataSet = dataset;
            dataset.map(function (page) {
                page.hars.map(function (har) {
                    har.config = {};
                    var mhs = har.name.match($.regex.harname);
                    if (mhs)
                        har.name.match($.regex.harname).map(function (v, i) {
                            har.config[current.harNames[i]] = v.trim() || '_';
                        });
                    else {
                        App.log('unparseable har hame: ', har.name);
                    }
                });
            });
        };
        $scope.onSelectHar = function (har) {
            har.selected = !(har.selected || false);
            if ($scope.allSelected) {
                $scope.allSelected = false;
            }
            if (har.selected) {
                $scope.selected.push(har);
            } else {
                $scope.selected.remove(har);
            }
        };
        $scope.selectAllHar = function (hars, allSelected) {
            for (var i = 0; i < hars.length; i++) {
                hars[i].selected = allSelected;
                if (hars[i].selected) {
                    if (!$.aContains($scope.selected, hars[i]))
                        $scope.selected.push(hars[i]);
                } else {
                    $scope.selected.remove(hars[i]);
                }
            }
        };
        $scope.onClear = function () {
            $scope.dataSet.map(function (page) {
                page.allSelected = false;
            });
            $scope.selected.map(function (har) {
                har.selected = false;
            });
            $scope.selected.length = 0;
        };
        $scope.onDelete = function () {
            $("#confirm_delete_har").hide();
            var harName = [];
            $scope.selected.map(function (har) {
                    harName.push(har.pageName+"/"+har.loc);
            });
            App.harClient.harDelete({
                callback: function onHarDelRes(data) {
                    if (data.stat) {
                        $.appSuccess(data.msg);
                    } else
                        $.appError(data.msg);
                },
                data: harName
            });
            current.init(current)($scope);
            $scope.onClear();
        };
        $scope.onCompare = function () {
            $scope.selected.map(function (har) {
                if (!current.data.config.selectedHars.find(function (char) {
                    return har.name === char.name;
                }))
                    current.data.config.selectedHars.push(har);
            });
            if (current.data.config.selectedHars.length > 0) {
                current.doCompare(current, $scope);
                $scope.$parent.homeView = current.views.harCompare;
            } else
                $.appError("Please select atleast one har!");
        };
        $scope.toggle = function (m, k) {
            if (k instanceof Array)
                k.map(function (k) {
                    $scope.toggle(m, k);
                });
            else
                m[k] = !((m[k] === undefined) ? true : m[k]);
        };
        $scope.initfilter = function (page) {
            page.filterD = {browser: {}, platform: {}};
            page.allSelected = false;
        };
        $scope.filterFn = function (fd) {
            return function (e, i, a) {
                return accept(fd.browser[e.config.browser], true)
                        && accept(fd.platform[e.config.platform], true);
            };
        };
        function accept(flag, def) {
            return (flag === undefined) ? def : flag;
        }
        $scope.flbtn = function (m) {
            return [m.Chrome, m.Safari, m.Firefox, m.IE].any(false);
        };
        $scope.flbtmP = function (m) {
            return [m.WIN7, m.MAC, m.LINUX].any(false);
        };
    }]);
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="harCompare">
app.controller('harCompare', ['$scope', 'current', '$timeout', function ($scope, current, $timeout) {
        $scope.d = {hars: []};
        $scope.ref = {};
        $scope.tolerance = 5;
        $scope.hideRef = false;
        $scope.editRef = false;
        $scope.sort = {};
        $scope.sort.options = [{key: 'name', name: 'Har Name'},
            {key: 'har.log.pages[0].pageTimings.onContentLoad', name: 'Content Load'},
            {key: 'har.log.pages[0].pageTimings.onLoad', name: 'Page Load'}];
        $scope.sort.with = $scope.sort.options[0];
        $scope.sort.by = false;
        $scope.resolvedLink = function (req) {
            return current.getLinkName(req);
        };
        $scope.resolvedLongLink = function (req) {
            return current.getLinkLongName(req);
        };
        $scope.setRef = function (e) {
            $scope.ref = current.data.config.ref = $.extend(true, {}, e);
            onRefUpdate();
            if (!$scope.ref.har.config.name) {
                $scope.ref.har.config.name = "New Reference";
            }
        };
        $scope.onImportRef = function () {
            if (current.data.refList && current.data.refList.length > 0)
                $scope.$parent.homeView = current.views.selectRef;
            else
                $.appError('No Benchmark to display!');
        };
        function onRefUpdate() {
            $scope.r_p = $scope.ref.har.log.pages[0].pageTimings;
            $scope.ref._assetMap = {};
            current.resolve.assetMap(current, $scope.ref, '_assetMap', $scope.ref._assetMap);
            indexHars();
        }
        function indexHars() {
            $scope.d.hars.map(function (e) {
                current.resolve.har(e.har);
                $scope.d._assets = {};
                current.resolve.assetMap(current, e, '_assetMap', $scope.d._assets);
                diff(e, $scope.ref);
            });
        }
        function onClearAll() {
            App.harClient.clearSelectedHars();
            $scope.$parent.homeView = current.views.selectHar;
        }
        $scope.removeHar = function (e) {
            $scope.d.hars.remove(e);
            var es = current.data.config.selectedHars.find(function (es) {
                return (es.name === e.name);
            });
            current.data.config.selectedHars.remove(es);
            if ($scope.d.hars.length < 1)
                onClearAll();
        };
        $scope.onDiff = function (e) {
            current.assetsToAdd = [];
            current.cAssetsMap = e._assetMap;
            current.newAssets = e._newAssets;
            $scope.$parent.homeView = current.views.selectAsset;
        };

        $scope.clearAll = function () {
            current.data.config.selectedHars.length = 0;
            $scope.d.hars.length = 0;
            onClearAll();
        };
        $scope.removeResource = function (e, index) {
            $scope.ref.har.log.entries.remove(e);
            $scope.d.hars.map(function (ele) {
                ele.har.log.entries.remove(ele.har.log.entries[index]);
            });
        };
        $scope.addResource = function (e, index) {
            var newR = $.extend(true, {}, current.resource);
            newR.startedDateTime = e.startedDateTime;
            $scope.ref.har.log.entries.push(newR);
        };
        $scope.checkTime = function (ref, val) {
            var dif = ((val - ref) / (ref / 100));
            return dif === 0 ? 0 : Math.sign($scope.tolerance - dif);
        };
        $scope.timeDiff = function (ref, val) {
            return current.diffStr(ref, val);
        };
        $scope.onSort = function () {
            $scope.d.hars.sort(current.sort($scope.sort.by)
                    .with({key: $scope.sort.with.key}));
        };
        $scope.onEditRef = function () {
            $scope.editRef = !$scope.editRef;
        };
        $scope.onRefSave = function () {
            $scope.ref.har.config.tolerance = $scope.tolerance;
            //set ref to true as save is done (avoid replacing saved the har in first time session)
            current.data.config.ref.ref = true;
            App.harClient.saveRefHar({
                callback: function onRefSaveRes(data) {
                    if (data.stat) {
                        $.appSuccess(data.name + ' successfully saved!');
                        current.data.refList = data.refList;
                    } else
                        $.appError('failed to save ' + data.name);
                },
                data: {
                    name: $scope.ref.har.config.name,
                    ext: '.har.ref',
                    har: $scope.ref.har
                }
            });
        };
        function diff(e, ref) {
            if (ref && ref._assetMap) {
                e._newAssets = [];
                Object.keys(e._assetMap).map(function (asset) {
                    if (!ref._assetMap[asset]) {
                        e._newAssets.push(asset);
                    }
                });
                if (e._newAssets.length < 1)
                    e._newAssets = false;
            }
        }

        $scope.$watch(function () {
            return current.assetsToAdd;
        }, function (n, o) {
            if (n && n.length > 0) {
                n.map(function (asset) {
                    if (!$scope.ref._assetMap[asset]) {
                        $scope.ref.har.log.entries.push(current.cAssetsMap[asset]);
                        $scope.ref._assetMap[asset] = current.cAssetsMap[asset];
                    }
                    $scope.d.hars.map(function (entry) {
                        if (entry._newAssets)
                            entry._newAssets.remove(asset);
                    });
                });
                $scope.d.hars.map(function (entry) {
                    if (entry._newAssets.length === 0) {
                        entry._newAssets = false;
                    }
                });

            }
        });
        $scope.$watch(function () {
            return current.data.config.ref;
        }, function (n, o) {
            if (n && n.har) {
                $scope.ref = current.data.config.ref;
                $scope.tolerance = $scope.ref.har.config.tolerance || $scope.tolerance;
                onRefUpdate();
            }
        });
        $scope.$watch(function () {
            return current.harsCompare.harsData;
        }, function (n, o) {
            if (n) {
                $scope.d.hars = n;
                if (!$scope.ref.ref) {
                    $scope.setRef($scope.d.hars[0]);
                } else {
                    indexHars();
                }
                $scope.d.hars.sort(current.sort($scope.sort.by).
                        with({key: $scope.sort.with.key}));
            }
        });
    }]);
//</editor-fold>

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="filters">
app.filter('filtrContains', function () {
    return function () {
        var gArg = arguments;
        return arguments[0].filter(function (e) {
            function evalx(exp) {
                try {
                    return eval(exp);
                } catch (ex) {
                    return;
                }
            }
            if (gArg.length === 2)
                return (e || '').containsx(gArg[1]);
            return (((e instanceof Object) ? (evalx('e.' + gArg[1]) || '') : e) || '').containsx(gArg[2]);
        });
    };
});
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="directives-element">
app.directive('imgSrc', function ($compile) {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            if (attr.imgSrc) {
                var val = attr.imgSrc, group = $scope;
                attr.dataType = $(ele).attr('data-type');
                if (attr.dataType) {
                    group = $scope[attr.dataType];
                }
                ele.on('click', function () {
                    attr.$set('src', '/media/images/' + val + '_' + group[val] + '.png');
                    $compile(ele)($scope);
                });
                attr.$set('src', '/media/images/' + val + '_true.png');
                attr.$set('img-src', null);
                $compile(ele)($scope);
            }
        }
    };
});
app.directive('imgSrci', function ($compile, current) {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            if (attr.imgSrci) {
                var config = ($scope.e || $scope.harPage.hars[+attr.imgSrci]).har.config;
                var val = config.browser;
                if (!current.cap.bro.known.any(config.browser.toLocaleLowerCase())) {
                    val = config.driver;
                    if (!current.cap.bro.known.any(config.driver.toLocaleLowerCase())) {
                        val = 'default';
                    }
                }
                try {
                    val = current.cap.files[val.toLocaleLowerCase()];
                } catch (e) {
                }
                attr.$set('src', '/media/images/' + val + '_true.png');
                attr.$set('img-srci', null);
                $compile(ele)($scope);
            }
        }
    };
});
app.directive('toolTip', function ($compile) {
    return {
        restrict: 'A',
        link: function ($scope, ele, attr) {
            $(ele).hover(function (a) {
                $(ele).tooltip({
                    placement: 'auto right'
                });
                $(ele).attr('title', "");
                $(ele).tooltip('show');
            }, function (a) {
                $(ele).tooltip('hide');
            });
        }
    };
});

//</editor-fold>

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="Jquery">
$(document).ready(function () {
    $(window).resize(setTableBody);
    var target = $(".table1-bodyScroll");
    $(".table-body").scroll(function () {
        if (!($(".table-cover").get(0).scrollWidth > $(".table-cover").innerWidth())) {
            $(".table-header").offset({left: 467.312345 + (-1 * this.scrollLeft)});
            target.prop("scrollTop", this.scrollTop);
        } else {
            $(".table-header").offset({left: 450.235545 - $(".table-cover").scrollLeft() + (-1 * this.scrollLeft)});
            target.prop("scrollTop", this.scrollTop);
        }
    });
    $(".compare").click(function () {
        setTimeout(function () {
            if ($(".table-body").get(0).scrollHeight > $(".table-body").height()) {
                $(".table1-body").css("width", "387px");
                $(".table-header").css("padding-right", "16px");
            } else {
                $(".table1-body").css("width", "370px");
                $(".table-header").css("padding-right", "0px");
            }
        }, 30);
    });
    $("body").mouseover(function () {
        if ($(".table-body").get(0).scrollWidth > $(".table-body").innerWidth()) {
            $(".table1-bodyScroll").css("max-height", "363px");
        } else {
            $(".table1-bodyScroll").css("max-height", "380px");
        }
        if (($(".table-body").get(0).scrollHeight > $(".table-body").height() - 6) && ($(".table-body").get(0).scrollHeight !== $(".table-body").height())) {
            $(".table1-body").css("width", "387px");
            $(".table-header").css("padding-right", "16px");
        } else {
            $(".table1-body").css("width", "370px");
            $(".table-header").css("padding-right", "0px");
        }
    });
    $("body").mouseup(function (e) {
        if ($(".selectRef.inner").is(':visible')) {
            if ($(e.target).closest(".selectRef.inner").length === 0 && e.which === 1) {
                $(".harComparePage").click();
            }
        }
    });
    $(document).keydown(function (e) {
        if ($(".selectRef.inner").is(':visible') && e.which === 27) {
            $(".harComparePage").click();
        }
    });
    $(".confirm_delete").click(function(){
        $("#confirm_delete_har").fadeOut();
    });
    $(".delete").click(function(){
        $("#confirm_delete_har").fadeIn();
    });
});//document close
function setTableBody() {
    $(".table1").height($(".report_table-container").height());
}
//</editor-fold>