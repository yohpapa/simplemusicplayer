/**
 * Application definition
 */
var lm = null;
var pb = null;
var app = {

    // Application Constructor
    initialize: function() {
        console.log("initialize");

        $('#page_albums').on('pagecreate', function() {
            console.log('#page_albums#pagecreate');

            isPageCreated = true;
            if(!isDeviceReady)
                return;

            this.initializeAlbumList();
        });

        $('#page_album_tracks').on('pageshow', function(event, data) {
            console.log('#page_album_tracks#pagecshow');

            var albumId = $(this).data('param').albumId;
            app.initializeTrackList(albumId);
        });

        this.bindEvents();
    },

    // Bind Event Listeners
    bindEvents: function() {
        console.log("bindEvents");
        $(document).on('deviceready', this.onDeviceReady);
    },

    // deviceready Event Handler
    onDeviceReady: function() {
        console.log("onDeviceReady");

        isDeviceReady = true;
        if(!isPageCreated)
            return;

        lm = cordova.require('cordova/plugin/list_manager');
        pb = cordova.require('cordova/plugin/playback_controller');

        // pb.setPlayStateChangedCallback(this.onPlayStateChanged);
        pb.setPlayStateChangedCallback(onPlayStateChanged);

        app.initializeAlbumList();
    },

    // Initialize the album's list
    initializeAlbumList: function() {
        console.log("initializeAlbumList");

        $('#settings').on('click', function() {
            console.log("#settings clicked");

            $.ajax({
                url: 'oss_licenses.txt',
                type: 'get',
                contentType: 'application/text',
                success: function(text) {
                    console.log('#settings ajax finished');
                    navigator.notification.alert(text.toString(), null, 'Licenses', 'OK');
                },
                error: function() {
                    console.log('#settings ajax failed.')
                }
            });
        });

        lm.getAlbumList(function(albums) {
            console.log("onSuccess");
            if(!albums) {
                console.log('The album is empty.')
                return;
            } else {
                $.each(albums, function(index, album) {
                    var item = '<li><a href="#page_album_tracks?id=' + album.id + '" data-transition="slide">' +
                    '<img src="' + album.artwork + '"/>' +
                    '<h4>' + album.name + '</h4>' +
                    '<p>' + album.artist + '</p>' +
                    '<span class="ui-li-count">' + album.numTracks + '</span></a></li>';
                    // console.log(item);
                    $('#list_albums').append(item);
                });
                $('#list_albums').listview('refresh');

                $('ul[id="list_albums"] a').on('click', function() {
                    console.log('#list_albums#click');

                    var url = $(this).attr("href");
                    var albumId = url.replace(/.*id=/, "");
                    console.log('url: ' + url);
                    console.log('albumId: ' + albumId);

                    $('#page_album_tracks').data('param', {albumId: albumId});
                });
            }

        }, function(err) {
            console.log("onFailed");
            console.log(err);
        });
    },

    // Initliaze the track's list
    initializeTrackList: function(albumId) {
        console.log('initializeTrackList albumId: ' + albumId);

        // Retrieve the track IDs belonging to the album which has the album ID.
        lm.getTrackList(albumId, function(data) {
            console.log('getTrackList#onSuccess');

            // Initialize display
            $('#list_tracks').empty();
            $('#artwork').attr('src', function() { return data.artwork; });
            $('#album_title').html(data.album);
            $.each(data.tracks, function(index, track) {
                var item = '<li><a href="#?id=' + track.id + '">' +
                    '<h4>' + track.title + '</h4>' +
                    '<p>' + track.duration + '</p>' +
                    '<span class="ui-li-aside"></span>' +
                    '</a></li>';
                // console.log(item);
                $('#list_tracks').append(item);
            });
            $('#list_tracks').listview('refresh');

            // Set the track ID's list to the playback controller to play them.
            var trackIds = [];
            $.each(data.tracks, function(index, track) {
                trackIds.push(track.id);
            });
            pb.setTracks(trackIds, function() {
                console.log('setTracks#onSuccess');

                $('ul[id="list_tracks"] li').on('click', function() {

                    // TODO: The index does not change...
                    var index = $(this).index();
                    console.log('#list_tracks#click index: ' + index);

                    // Set the index of track which we would like to play soon.
                    console.log('index: ' + index);
                    pb.setIndex(index, function() {
                        console.log('setIndex#onSuccess');

                        // OK, we are ready to start playing the tracks.
                        pb.togglePlayPause(function() {
                            console.log('togglePlayPause#onSuccess');
                        }, function(err) {
                            console.log('togglePlayPause#onFailed');
                            console.log(err);
                        });

                    }, function(err) {
                        console.log('setIndex#onFailed');
                        console.log(err);
                    });
                });

            }, function(err) {
                console.log('setTracks#onFailed');
                console.log(err);
            });

        }, function(err) {
            console.log('getTrackList#onFailed');
            console.log(err);
        })
    }
};

function onPlayStateChanged(parameter) {
    console.log('onPlayStateChanged state: ' + parameter.state + ", index: " + parameter.index);

    if($.mobile.activePage.attr('id') != 'page_album_tracks') {
        console.log('This page is not page_album_tracks.');
        return;
    }

    var playState = parameter.state == 0 ? "Playing" : "Paused"
    $('ul[id="list_tracks"] li:nth-child(' + (parameter.index + 1) + ') span[class="ui-li-aside"]').html(playState);

    $('ul[id="list_tracks"] li').each(function(i, element) {
        if(i != parameter.index) {
            $('ul[id="list_tracks"] li:nth-child(' + (i + 1) + ') span[class="ui-li-aside"]').html("");
        }
    });
}

var isDeviceReady = false;
var isPageCreated = false;
