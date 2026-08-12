function forEachIn(obj, callback) {
    for (key in obj) {
        if (obj.hasOwnProperty(key)) {
            callback(key, obj[key]);
        }
    }
}


var _pageItems = [];
var _selectedIds = [];
var _pageName;
function removeItemFromSelectedList(id) {
    for (var i = 0; i < _selectedIds.length; i++) {
        if (_selectedIds[i] == id) {
            _selectedIds.splice(i, 1);
        }
    }
}

function distanceInsideRect(area, x, y) {
    if (x >= area[0] && x <= area[0] + area[2]
            && y >= area[1] && y <= area[1] + area[3]) {

        var distance = Math.min(x - area[0], area[0] + area[2] - x)
                + Math.min(y - area[1], area[1] + area[3] - y);
        return distance;
    }
    else
        return -1;
}
function sortItemsByName(items) {
    for (var i = 0; i < items.length - 1; i++) {
        for (var j = i + 1; j < items.length; j++) {
            if (items[i].name > items[j].name) {
                var temp = items[i];
                items[i] = items[j];
                items[j] = temp;
            }
        }
    }
    return items;
}
function initGalenPageDump(pageData) {
    removeAll();
    _pageName = pageData.pageName;
    var canvas = $(".image .canvas");
    for (objectName in pageData.items) {
        if (pageData.items.hasOwnProperty(objectName)) {
            var item = pageData.items[objectName];
            item.name = objectName;
            item.id = _pageItems.length;
            item.selected = false;
            _pageItems.push(item);
        }
    }

    //sorting items by name
    var sortedItems = sortItemsByName(_pageItems);
    for (var i = 0; i < sortedItems.length; i++) {
        sortedItems[i].id = i;
        renderPageItem(canvas, sortedItems[i]);
    }
    canvas = document.querySelector("#imageview > div.canvas");
    canvas.removeEventListener("click", clickCanvas);
    canvas.addEventListener("click", clickCanvas);
}

function clickCanvas(event) {
    var offset = $(this).offset();
    var x = event.pageX - offset.left;
    var y = event.pageY - offset.top;

    //Finding the closest to the rect

    var minDistance = 1000000;
    var selectedId = -1;
    for (var i = 0; i < _pageItems.length; i++) {
        var distance = distanceInsideRect(_pageItems[i].area, x, y);
        if (distance > -1 && distance < minDistance) {
            selectedId = i;
            minDistance = distance;
        }
    }

    if (selectedId >= 0) {
        selectItem(selectedId);
    }

}

function selectItem(id) {

    _pageItems[id].selected = !_pageItems[id].selected;
    var item = _pageItems[id];

    var $item = $("#canvas-brick-" + item.id);
    var $listItem = $("#object-list-item-" + item.id);

    if (item.selected) {
        _selectedIds.push(id);

        $item.addClass("selected");
        $listItem.addClass("selected");

    } else {
        removeItemFromSelectedList(id);
        $item.removeClass("selected");
        $listItem.removeClass("selected");
    }

    if (_selectedIds.length >= 2) {
        showSpecSuggestions(_selectedIds);
    }
    else if (_selectedIds.length === 1) {
        showObjectDetails(_pageItems[_selectedIds[0]]);
        //hideSpecSuggestions();
    }
    else if (_selectedIds.length === 0) {
        hideObjectDetails();
        hideSpecSuggestions();
    }
}

function hideSpecSuggestions() {
    $("#object-suggestions").hide();
}

function Point(x, y) {
    this.x = x;
    this.y = y;
}
Point.prototype.isInsideArea = function(area) {
    return this.x >= area[0] && this.x < area[0] + area[2]
            && this.y >= area[1] && this.y < area[1] + area[3];
}
function areaToPoints(area) {
    return [
        new Point(area[0], area[1]),
        new Point(area[0] + area[2], area[1]),
        new Point(area[0] + area[2], area[1] + area[3]),
        new Point(area[0], area[1] + area[3])
    ];
}

function Spec(objectName, action,input,optional) {
    this.objectName = objectName;
    this.action = action;
	 this.input = input&&input.trim().length>0?"@"+input:"";
	  this.optional = optional;
}

function __galen_textForLocationsOfInsideSpec(itemA, itemB) {
    var sides = {
        top: itemA.area[1] - itemB.area[1],
        left: itemA.area[0] - itemB.area[0],
        right: itemB.area[0] + itemB.area[2] - itemA.area[0] - itemA.area[2],
        bottom: itemB.area[1] + itemB.area[3] - itemA.area[1] - itemA.area[3]
    }

    var text = "";
    var haveFirst = false;
    forEachIn(sides, function(sideName, value) {
        if (value > 0) {
            if (haveFirst) {
                text = text + ",";
            }
            haveFirst = true;

            text = text + " " + value + "px " + sideName;
        }
    });

    return text;
}

function Suggest() {
    this.specs = {};
}
Suggest.prototype.addSpec = function(spec) {
    if (this.specs[spec.objectName] === undefined) {
        this.specs[spec.objectName] = [spec];
    }
    else
        this.specs[spec.objectName].push(spec);
};
Suggest.prototype.generateSuggestions = function(items) {
    var thisSuggest = this;
    var propose = function(spec) {
        if (spec != null) {
            thisSuggest.addSpec(spec);
        }
    };
    forEachIn(this._suggestions, function(name, suggestion) {
        for (var i = 0; i < items.length - 1; i++) {
            for (var j = i + 1; j < items.length; j++) {
                propose(suggestion(items[i], items[j]));
                propose(suggestion(items[j], items[i]));
            }
        }
    });

    var html = getSpecSuggestionTable(this.specs);
    return "<pre>" + html + "</pre>";
};
Suggest.prototype._suggestions = {};
Suggest.prototype._suggestions.width = function(a, b) {
    return new Spec(a.name, "assertElementWidthElement", getPercentage(a.area[2], b.area[2]) + "% " , b.name);
};
Suggest.prototype._suggestions.height = function(a, b) {
    return new Spec(a.name, "assertElementHeightElement", getPercentage(a.area[3], b.area[3]) + "% " , b.name);
};
Suggest.prototype._suggestions.inside = function(itemA, itemB) {
    var points = areaToPoints(itemA.area);
    for (var i = 0; i < points.length; i++) {
        if (!points[i].isInsideArea(itemB.area)) {
            return null;
        }
    }
    return new Spec(itemA.name, "assertElementInside", __galen_textForLocationsOfInsideSpec(itemA, itemB), itemB.name);
};
Suggest.prototype._suggestions.insidePartly = function(itemA, itemB) {
    var points = areaToPoints(itemA.area);

    var amountOfInsidePoints = 0;
    for (var i = 0; i < points.length; i++) {
        if (points[i].isInsideArea(itemB.area)) {
            amountOfInsidePoints++;
        }
    }

    if (amountOfInsidePoints > 0 && amountOfInsidePoints < 4) {
        return new Spec(itemA.name, "assertElementInsidePartly",__galen_textForLocationsOfInsideSpec(itemA, itemB) , itemB.name);
    }
    return null;
};
Suggest.prototype._suggestions.alignedHorizontally = function(itemA, itemB) {
    var dTop = Math.abs(itemA.area[1] - itemB.area[1]);
    var dBottom = Math.abs(itemA.area[1] + itemA.area[3] - itemB.area[1] - itemB.area[3]);

    if (dTop < 5 && dBottom < 5) {
        var errorRate = "";
        if (dTop > 0 || dBottom > 0) {
            errorRate = "all " + Math.max(dTop, dBottom) + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedHoriz", errorRate ,itemB.name);
    }
    else if (dTop < 5) {
        var errorRate = "";
        if (dTop > 0) {
            errorRate = "top " + dTop + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedHoriz", errorRate ,itemB.name);
    }
    else if (dBottom < 5) {
        var errorRate = "";
        if (dBottom > 0) {
            errorRate = "bottom " + dBottom + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedHoriz", errorRate , itemB.name);
    }
    return null;
};
Suggest.prototype._suggestions.alignedVertically = function(itemA, itemB) {
    var dLeft = Math.abs(itemA.area[0] - itemB.area[0]);
    var dRight = Math.abs(itemA.area[0] + itemA.area[2] - itemB.area[0] - itemB.area[2]);

    if (dLeft < 5 && dRight < 5) {
        var errorRate = "";
        if (dLeft > 0 || dRight > 0) {
            errorRate = "all " + Math.max(dLeft, dRight) + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedVert", errorRate , itemB.name);
    }
    else if (dLeft < 5) {
        var errorRate = "";
        if (dLeft > 0) {
            errorRate = "left " + dLeft + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedVert",errorRate, itemB.name);
    }
    else if (dRight < 5) {
        var errorRate = "";
        if (dRight > 0) {
            errorRate = "right " + dRight + "px";
        }
        return new Spec(itemA.name, "assertElementAlignedVert",  errorRate , itemB.name);
    }
    return null;
};
Suggest.prototype._suggestions.above = function(a, b) {
    var diff = b.area[1] - a.area[1] - a.area[3];
    if (diff >= 0) {
        return new Spec(a.name, "assertElementAbove", diff + "px" , b.name);
    }
    return null;
};
Suggest.prototype._suggestions.below = function(a, b) {
    var diff = a.area[1] - b.area[1] - b.area[3];
    if (diff >= 0) {
        return new Spec(a.name, "assertElementBelow", diff + "px" , b.name);
    }
    return null;
};
Suggest.prototype._suggestions.leftOf = function(a, b) {
    var diff = b.area[0] - a.area[0] - a.area[2];
    if (diff >= 0) {
        return new Spec(a.name, "assertElementLeftOf", diff + "px" , b.name);
    }
    return null;
};
Suggest.prototype._suggestions.rightOf = function(a, b) {
    var diff = a.area[0] - b.area[0] - b.area[2];
    if (diff >= 0) {
        return new Spec(a.name, "assertElementRightOf", diff + "px" , b.name);
    }
    return null;
};
Suggest.prototype._suggestions.centered = function(a, b) {
    //centered all inside
    //centered hor in
    //cent v in
    //cent hor on
    //cent v on
    var dt = a.area[1] - b.area[1];
    var db = b.area[1] + b.area[3] - a.area[1] - a.area[3];

    var dl = a.area[0] - b.area[0];
    var dr = b.area[0] + b.area[2] - a.area[0] - a.area[2];

    var similar = function(value1, value2) {
        return Math.abs(value1 - value2) < 5;
    };

    var errorRate = function(diff) {
        var absDiff = Math.abs(diff);
        if (absDiff > 0) {
            return " " + absDiff + "px";
        }
        return "";
    };

    if (similar(dt, db) && dt > 0) {
        if (similar(dl, dr) && dr > 0) {
		    return new Spec(a.name, "assertElementCenteredAInside",errorRate(Math.max(Math.abs(dt - db), Math.abs(dl - dr))) , b.name);
        }
        else {
            return new Spec(a.name, "assertElementCenteredVInside", errorRate(dt - db) , b.name);
        }
    }
    else if (similar(dr, dl) && dr > 0) {
        return new Spec(a.name, "assertElementCenteredHInside", errorRate(dl - dr) , b.name);
    }
    else if (similar(dt, db) && dt < 0) {
        return new Spec(a.name, "assertElementCenteredVOn", errorRate(dt - db) , b.name);
    }
    else if (similar(dr, dl) && dr < 0) {
        return new Spec(a.name, "assertElementCenteredHOn", errorRate(dr - dl) , b.name);
    }
    return null;
};

Suggest.prototype.generateSuggestion = function(item) {
    var thisSuggest = this;
    var propose = function(spec) {
        if (spec !== null) {
            thisSuggest.addSpec(spec);
        }
    };
    forEachIn(this._suggestion, function(name, suggestion) {
        propose(suggestion(item));
    });

    var html = getSpecSuggestionTable(this.specs);
    return "<pre>" + html + "</pre>";
};
Suggest.prototype._suggestion = {};
Suggest.prototype._suggestion.width = function(a) {
    return new Spec(a.name, "assertElementWidth", a.area[2] + "px ","");
};
Suggest.prototype._suggestion.height = function(a) {
    return new Spec(a.name, "assertElementHeight", a.area[3] + "px ","");
};
Suggest.prototype._suggestion.text = function(a) {
    if (a.text)
        return new Spec(a.name, "assertElementTextContains", a.text,"");
    return null;
};
function getPercentage(a, b) {
    var value = (a / b) * 100;
    return parseFloat(value.toPrecision(3));
}

function getSpecSuggestionTable(Specs)
{
var html = "";
for (var objectName in Specs) {
        if (Specs.hasOwnProperty(objectName)) {
            html += "\n<b>" + objectName + "</b>\n";
            var specs = Specs[objectName];
			html+="<table>";
			   html += "<tr><th>ObjectName</th>"
			   +"<td></td>"
				+"<th>Action </th>"
				+"<th>Input</th>"
				+"<th>Condition</th>"
				+"<th>Reference</th></tr>";
             for (var i = 0; i < specs.length; i++) {
                html += "<tr><td>" + specs[i].objectName +"</td>"
				+"<td></td>"
				+"<td>" + specs[i].action +"</td>"
				+"<td>" + specs[i].input +"</td>"
				+"<td>" + specs[i].optional +"</td>"
				+"<td>" + $(".page-array").select2('val') +"</td></tr>";
            }
			html+="</table>";
        }
    }
	return html;
}

function showSpecSuggestions(selectedIds) {

    var suggest = new Suggest();

    var items = [];

    for (var i = 0; i < selectedIds.length; i++) {
        items.push(_pageItems[selectedIds[i]]);
    }

    $("#object-suggestions .spec-list").html(suggest.generateSuggestions(items));
    $("#object-suggestions").show();
}

function showObjectDetails(item) {
    var suggest = new Suggest();

    $("#object-details .xf-object-name").text(item.name);
    $("#object-suggestions .spec-list").html(suggest.generateSuggestion(item));
    $("#object-suggestions").show();

    if (item.hasImage) {
        $("#object-details .image").html("<img src='" +currTcName+"/"+ _pageName + "/objects/" + item.name + ".png'/>").show();
    }
    else
        $("#object-details .image").hide();

    $("#object-details").show();
}

function hideObjectDetails() {
    $("#object-details").hide();
}

function renderPageItem(canvas, item) {
    $("#object-list ul").append($("<li id='object-list-item-" + item.id + "' data-id='" + item.id + "'>" + item.name + "</li>").click(function() {
        var id = $(this).attr("data-id");
        selectItem(id);
    }));

    canvas.append("<div class='brick' id='canvas-brick-" + item.id + "' "
            + "style='"
            + "left:" + item.area[0] + "px;"
            + "top:" + item.area[1] + "px;"
            + "width:" + item.area[2] + "px;"
            + "height:" + item.area[3] + "px;"
            + "'>"
            + "<span>" + item.name + "</span>"
            + "</div>");
}

function removeAll() {
    _pageItems = [];
    _selectedIds = [];
    var canvas = $(".image .canvas")[0];
    var objectList = $("#object-list ul")[0];
    removeAllChild(canvas);
    removeAllChild(objectList);
}

function removeAllChild(el) {
    if (el)
        while (el.hasChildNodes()) {
            el.removeChild(el.lastChild);
        }
}