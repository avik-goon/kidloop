package com.kidloop.app.player

object YouTubeHtmlBuilder {

    /**
     * Builds the full HTML document containing the YouTube iframe embed with
     * required permissions (autoplay, encrypted-media) and the IFrame API script bridge.
     */
    fun build(initialItemId: String, isPlaylist: Boolean): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                html, body {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: #000000;
                    overflow: hidden;
                    position: fixed;
                }
                #player {
                    width: 100%;
                    height: 100%;
                    position: absolute;
                    top: 0;
                    left: 0;
                }
            </style>
            <!-- defer ensures DOM elements and JS declarations are parsed before iframe_api executes -->
            <script defer src="https://www.youtube.com/iframe_api"></script>
        </head>
        <body>
            <div id="player"></div>

            <script type="text/javascript">
                var player;
                var isReady = false;

                function onYouTubeIframeAPIReady() {
                    console.log("KidLoop JS: onYouTubeIframeAPIReady called");
                    var playerVars = {
                        'autoplay': 1,
                        'controls': 0,
                        'disablekb': 1,
                        'fs': 0,
                        'rel': 0,
                        'playsinline': 1,
                        'iv_load_policy': 3,
                        'enablejsapi': 1,
                        'origin': 'https://com.kidloop.app'
                    };

                    var config = {
                        height: '100%',
                        width: '100%',
                        playerVars: playerVars,
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    };

                    if ('$isPlaylist' === 'true') {
                        config.playerVars.listType = 'playlist';
                        config.playerVars.list = '$initialItemId';
                    } else {
                        config.videoId = '$initialItemId';
                    }

                    try {
                        player = new YT.Player('player', config);
                        console.log("KidLoop JS: YT.Player instantiated successfully");
                    } catch (e) {
                        console.error("KidLoop JS: Exception in new YT.Player: " + e);
                    }
                }

                function onPlayerReady(event) {
                    console.log("KidLoop JS: onPlayerReady called");
                    isReady = true;
                    try {
                        event.target.playVideo();
                    } catch(e) {
                        console.error("KidLoop JS: playVideo error: " + e);
                    }
                    if (window.KidLoopBridge && window.KidLoopBridge.onPlayerReady) {
                        window.KidLoopBridge.onPlayerReady();
                    }
                }

                function onPlayerStateChange(event) {
                    console.log("KidLoop JS: onPlayerStateChange -> " + event.data);
                    if (window.KidLoopBridge && window.KidLoopBridge.onPlayerStateChange) {
                        window.KidLoopBridge.onPlayerStateChange(event.data);
                    }
                }

                function onPlayerError(event) {
                    console.error("KidLoop JS: onPlayerError -> " + event.data);
                    if (window.KidLoopBridge && window.KidLoopBridge.onPlayerError) {
                        window.KidLoopBridge.onPlayerError(event.data);
                    }
                }

                function loadVideo(id) {
                    console.log("KidLoop JS: loadVideo -> " + id);
                    if (player && typeof player.loadVideoById === 'function') {
                        player.loadVideoById(id);
                        try {
                            player.playVideo();
                        } catch(e) {}
                    }
                }

                function loadPlaylist(id) {
                    console.log("KidLoop JS: loadPlaylist -> " + id);
                    if (player && typeof player.loadPlaylist === 'function') {
                        player.loadPlaylist({
                            list: id,
                            listType: 'playlist'
                        });
                        try {
                            player.playVideo();
                        } catch(e) {}
                    }
                }

                function play() {
                    console.log("KidLoop JS: play called");
                    if (player && typeof player.playVideo === 'function') {
                        player.playVideo();
                    }
                }

                function pause() {
                    console.log("KidLoop JS: pause called");
                    if (player && typeof player.pauseVideo === 'function') {
                        player.pauseVideo();
                    }
                }

                // If iframe_api was already cached/loaded, initialize immediately
                if (window.YT && window.YT.Player) {
                    onYouTubeIframeAPIReady();
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
