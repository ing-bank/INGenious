

/**
 * 
 * @param {type} keyLength
 * @returns {String}
 */
$.RANDOM = {
    "get": function generateKey(keyLength) {
        var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz*&-%/!?*+=()";
        var randomstring = '';
        for (var i = 0; i < keyLength; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            randomstring += chars.substring(rnum, rnum + 1);
        }
        return randomstring;
    }
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
$.if = function (flag, res) {
    return flag ? res : "";
};
$.isLocal = function () {
    return window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
};
$.aContains = function (array, val) {
    return array.indexOf(val) !== -1;
};
$.onServerStopped = function () {
    toastr.error("Server Stopped");
};
$.onServerClose = function (event) {
    alert('Server connection closed\nPls reload to reconnect!');
};
$.onServerError = function (msg) {
    toastr.error(msg, "Server");
};
$.onServerMsg = function (msg) {
    toastr.info(msg);
};
$.onConnError = function (msg) {
    toastr.error(msg, "Connection Error");
};
$.appError = function (msg) {
    toastr.error(msg);
};
$.appSuccess = function (msg) {
    toastr.success(msg, '', {timeOut: 1000});
};
toastr.options.positionClass = 'toast-bottom-right';
$.WEBUI_API_PATH = '/dashboard/har';
$.SET_COOKIE = false;
$.SERVER_PUBLIC_KEY = '';
//$.SECRET_KEY = $.RANDOM.get(16);
$.JSEnc = {
    // "UI_ASYM": new JSEncrypt()
};
//$.JSEnc.UI_ASYM.setPublicKey($.SERVER_PUBLIC_KEY);
//<editor-fold defaultstate="collapsed" desc="Api Tags">


//</editor-fold>


//<editor-fold defaultstate="collapsed" desc="Api convinent library">
/**
 * return adds auth tokens
 * @returns {data}
 */

//</editor-fold>

$.regex = {};
$.regex.harname = /^(\d{2}-\w{3}-\d{4} \d{2}-\d{2}-\d{2})_(.*)_\[i(\d+) (.*)v(\d+[\.\d+]+|) (.*)\]_(\d+)_(.*)$/;
//<editor-fold defaultstate="collapsed" desc="Custom String Prototypes">

String.prototype.escapePath = function () {
    return this.replace(/\\\\/g, '/').replace(/\\/g, '/');
};
/**
 * prototype for String.replaceAll()
 * @param {type} find string to replace
 * @param {type} replace replacement
 * @returns {String.prototype.replaceAll.replace}
 */
String.prototype.replaceAll = function (find, replace) {
    return this.replace(new RegExp(
            find.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'g'), replace);
};
/**
 * return string contains substring
 * @param {type} str
 * @returns {Boolean}
 */
String.prototype.contains = function (str) {
    return this.indexOf(str) !== -1;
};
/**
 * return string contains substring (ignore case)
 * @param {type} str
 * @returns {Boolean}
 */
String.prototype.containsx = function (str) {
    return new RegExp(str, 'i').test(this);
};

/**
 * 
 * @returns {String.prototype@call;replaceAll@call;replaceAll}
 */
String.prototype.rawMAC = function () {
    return this.replace(/\-/g, '').replace(/\:/g, '');
};
/**
 * 
 * @returns string with space at the end
 */
String.prototype.gap = function () {
    return this.trim() + " ";
};
/**
 * 
 * @param {type} v value to remove
 * @returns status
 */
Array.prototype.remove = function (v) {
    var i = this.indexOf(v);
    if (i >= 0) {
        return  this.splice(i, 1);
    }
};

Array.prototype.any = function (flag) {
    function accept(val, flag) {
        return val === flag;
    }
    var list = Object(this);
    var length = list.length >>> 0;
    for (var i = 0; i < length; i++) {
        if (accept(list[i], flag))
            return true;
    }
    return false;
};
Array.prototype.all = function (flag) {
    function accept(val, flag) {
        return val !== flag;
    }
    var list = Object(this);
    var length = list.length >>> 0;
    for (var i = 0; i < length; i++) {
        if (accept(list[i], flag))
            return false;
    }
    return true;
};
/**
 * 
 * @returns string with first letter Caps and lower follows
 */
String.prototype.capFirst = function () {
    return this.charAt(0).toUpperCase() + this.slice(1).toLowerCase();
};
/**
 * return Encrypted String using Public Key
 * @returns {String}
 */
String.prototype.encryptKey = function () {
    return $.JSEnc.UI_ASYM.encrypt(this);
};
/**
 * Set the String as secret key
 * @returns {String}
 */
String.prototype.SetAsSecret = function () {
    $.SECRET_KEY = this;
};
/**
 * return Encrypted String using Secret Key
 * @returns {String}
 */
String.prototype.encrypt = function () {
    return $.JSEnc.UI_SYMM.encrypt(this);
};
/**
 * return Encrypted String using Secret Key
 * @returns {String}
 */
String.prototype.decrypt = function () {
    return $.JSEnc.UI_SYMM.decrypt(this);
};
//</editor-fold>
$.openNew = function (url) {
    var win = window.open(url, '_blank');
    win.focus();
};

if (!Math.sign) {
    (Math.prototype || Math).sign = function (v) {
        return v > 0 ? 1 : (v < 0) ? -1 : 0;
    };
}
/**
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find
 */
if (!Array.prototype.find) {
    Array.prototype.find = function (predicate) {
        if (this === null) {
            throw new TypeError('Array.prototype.find called on null or undefined');
        }
        if (typeof predicate !== 'function') {
            throw new TypeError('predicate must be a function');
        }
        var list = Object(this);
        var length = list.length >>> 0;
        var thisArg = arguments[1];
        var value;

        for (var i = 0; i < length; i++) {
            value = list[i];
            if (predicate.call(thisArg, value, i, list)) {
                return value;
            }
        }
        return undefined;
    };
}