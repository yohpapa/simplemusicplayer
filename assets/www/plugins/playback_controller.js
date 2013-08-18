cordova.define('cordova/plugin/playback_controller', function(require, exports, module) {
    var exec = require('cordova/exec');

    var PlaybackController = function() {};

    PlaybackController.prototype.setTracks = function(trackIds, onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'setTracks', trackIds);
    }

    PlaybackController.prototype.setIndex = function(index, onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'setIndex', [index]);
    }

    PlaybackController.prototype.setPlayStateChangedCallback = function(onChanged) {
        exec(onChanged, function(err) {
            console.log(err);
        }, 'PlaybackController', 'setPlayStateChangedCallback', []);
    }

    PlaybackController.prototype.playTrack = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'playTrack', []);
    }

    PlaybackController.prototype.pauseTrack = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'pauseTrack', []);
    }

    PlaybackController.prototype.togglePlayPause = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'togglePlayPause', []);
    }

    PlaybackController.prototype.nextTrack = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'nextTrack', []);
    }

    PlaybackController.prototype.prevTrack = function(onSuccess, onFailed) {
        exec(onSuccess, onFailed, 'PlaybackController', 'prevTrack', []);
    }

    var playbackController = new PlaybackController();
    module.exports = playbackController;
});