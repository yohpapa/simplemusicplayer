cordova.define('cordova/plugin/list_manager', function(require, exports, module) {
    var exec = require('cordova/exec');

    var ListManager = function() {};

    ListManager.prototype.getAlbumList = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'ListManager', 'get_album_info', []);
    }

    ListManager.prototype.getTrackList = function(albumId, onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'ListManager', 'get_track_info', [albumId]);
    }

    var listManager = new ListManager();
    module.exports = listManager;
});