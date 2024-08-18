

function getRawHTML(html) {
    var s = html.replace(/\</g, '&lt;');
    return s.replace(/\>/g, '&gt;');
}

function setImpact() {
    if ($("#data_mode").val() === "Passes") {
        $("#impact").removeAttr("class");
        $("#impact").html("NA");

    } else {
        $("#impact").removeAttr("class");
        $("#impact").html($(".current_vio > .current_node").attr("impact"));
        $("#impact").addClass($(".current_vio > .current_node").attr("impact"));
    }
}

function getRelatedElem(ele) {
    var vio = "<p><label>Related Nodes:</label></p>";
    vio += "<ul>";
    $.each(ele, function (r, s) {
        vio += "<li>" + getRawHTML(s.html) + "</li>";
    });
    vio += "</ul>";
    return vio;
}
function getTags(tags) {
    var vals = "<div id = \"tags\" style = \"float: right;\">";
    $.each(tags, function (k, v) {
        vals += "<text>" + v + "</text>";
    });
    vals += "</div>";
    return vals;
}
function getList(ele) {
    var list = "";
    $.each(ele, function (key, val) {
        var style;
        (val.nodes.length < 10) ? style = "margin: 4px 7px;" : style = "";
        list += "<li id = \"vio_" + key + "\" class = \"vioListCall\" nodeCount = \"" + val.nodes.length + "\">\n\
            <p title = '" + val.help + "'>" + getRawHTML(val.help) + "</p><span class = \"total-violation\">\n\
            <label style = \"" + style + "\">" + val.nodes.length + "</label></span></li>";
    });
    return list;
}
function getResults(ele) {
    var violations = "";
    $.each(ele, function (key, val) {
        if (key === 0) {
            violations += "<div class = \"vio_" + key + " current_vio\" style = \"display : block;\">";
        } else {
            violations += "<div class = \"vio_" + key + "\" style = \"display : none;\">";
        }
        $.each(val.nodes, function (k, v) {
            if (k === 0) {
                violations += "<div class = \"nodes-group current_node\" nodeNo = \"" + (k + 1) + "\" impact = \"" + v.impact + "\" style = \"display: none;\"><div class = \"node-data\">\n\
                    <text>" + getRawHTML(val.description) + "</text>" + getTags(val.tags) + "\n\
                    <br/>(<a href = \"" + val.helpUrl + "\">More Info</a>)</div>\n\
                    <div class = \"node-data\"><label>Target: </label><span>" + v.target + "</span></div>\n\
                    <div class = \"node-data\"><label>HTML: </label><br/>\n\
                    <div class = \"highlight-pane highlight-pane-error\"><textarea height = \"auto\" disabled style = \"padding: 5px 5px 6px;\n\
                    width: 98%;\">" + v.html + "</textarea></div></div><div class = \"node-data\">";
            } else {
                violations += "<div class = \"nodes-group\" nodeNo = \"" + (k + 1) + "\" impact = \"" + v.impact + "\" style = \"display: none;\"><div class = \"node-data\">\n\
                    <text>" + getRawHTML(val.description) + "</text>" + getTags(val.tags) + "<br/>(<a href = \"" + val.helpUrl + "\">More Info</a>)</div>\n\
                    <div class = \"node-data\"><label>Target: </label><span>" + v.target + "</span></div>\n\
                    <div class = \"node-data\"><label>HTML: </label><br/>\n\
                    <div class = \"highlight-pane highlight-pane-error\"><textarea height = \"auto\" disabled style = \"padding: 5px 5px 6px;\n\
                    width: 98%;\">" + v.html + "</textarea></div></div><div class = \"node-data\">";
            }

            violations += "<p style = \"margin-top: 0px;\"><label>Summary:</label></p>";
            if (v.any.length > 0) {
                violations += "<label>Fix any of the following:</label><br/><ul>";
                $.each(v.any, function (p, q) {
                    violations += "<li>" + getRawHTML(q.message) + "</li>";
                    if (q.relatedNodes.length > 0) {
                        violations += getRelatedElem(q.relatedNodes);
                    }
                });
                violations += "</ul>";
            }

            if (v.none.length > 0) {
                violations += "<br/><label>Fix all of the following:</label><br/><ul>";
                $.each(v.none, function (p, q) {
                    violations += "<li>" + getRawHTML(q.message) + "</li>";
                    if (q.relatedNodes.length > 0) {
                        violations += getRelatedElem(q.relatedNodes);
                    }
                });
                violations += "</ul>";
            }
            violations += "</div></div>";
        });
        violations += "</div>";
    });
    return violations;
}
function init() {	
	var val=$("#test_type").val();
	data = JSON.parse(dataList[val]);	
    $("#url > input").attr("value", data.url);
    $(".vio-list").html(getList(data.violations));
    $("#issue-details").html(getResults(data.violations));	
	$("#data_mode").val("Violations");
	$(".vioListCall").first().trigger( "click" );
}

$(document).ready(function () {
	$.each(dataList, function (key, value) {
		$('<option>').val(key).text(key).appendTo('#test_type');
	});
    init();
    $("#test_type").change(function () {		
		init();
    });
    $("#data_mode").change(function () {

        if ($(this).val() === "Passes") {
            $(".vio-list").html(getList(data.passes));
            $("#issue-details").html(getResults(data.passes));
            $("#mode").html("Pass");
			$(".highlight-pane").removeClass("highlight-pane-error");
        } else
        if ($(this).val() === "Violations") {
            $(".vio-list").html(getList(data.violations));
            $("#issue-details").html(getResults(data.violations));
            $("#mode").html("Violation");
			$(".highlight-pane").addClass("highlight-pane-error");
        }
        $(".vioListCall").first().addClass("current_vio_list");
        $("#nodeCount").html($(".current_vio_list").attr("nodeCount"));
        $("#currentNode").html($(".current_vio > .current_node").attr("nodeNo"));
        setImpact();
    });


    $(".vio-list").on("click", ".vioListCall", function () {
        var id = $(this).attr("id");
        $(".current_vio_list").removeClass("current_vio_list");
        $(this).addClass("current_vio_list");
        $("#nodeCount").html($(this).attr("nodeCount"));
        $(".current_vio").css("display", "none");
        $(".current_vio").removeClass("current_vio");
        $("." + id).css("display", "block");
        $("." + id).addClass("current_vio");
        $("#currentNode").html($(".current_vio > .current_node").attr("nodeNo"));
        setImpact();
    });
    $(".prev").click(function () {
        var node = $(".current_vio > .current_node").prev('.nodes-group');
        if (node.length > 0) {
            $("#currentNode").html(parseInt($("#currentNode").html()) - 1);
            node.addClass("current_node");
            $(".current_vio > .current_node").last().removeClass("current_node");
            setImpact();
        }
    });
    $(".next").click(function () {
        var node = $(".current_vio > .current_node").next('.nodes-group');
        if (node.length > 0) {
            $("#currentNode").html(parseInt($("#currentNode").html()) + 1);
            node.addClass("current_node");
            $(".current_vio > .current_node").first().removeClass("current_node");
            setImpact();
        }
    });
	
	$(".vioListCall").first().trigger( "click" );
});