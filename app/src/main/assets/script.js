$( window ).load( function() {
    if (page === "index") {
        loadJson();
    } else {
        adjustTable();
    }
    setInterval(loadJson, 5000);
    $('.input').click(function() {
        if ($(this).prop("value") === "Add a link here...") $(this).prop("value", "");
    });
});

function loadJson () {
    var downloads = $("#content.downloads");
    var message   = $("#message");
    var downloads_array = [];

    $.getJSON("list", function(data) {

        $.each(data["downloadlist"], function(index, elem) {
            var items = [];

            var size = elem['size'];
            var progress = elem['progress'];
            var percent = 0; // computing this later
            var status = elem['status'];

            var action_play = "<img alt='Play' title='Play' src='icon_play.png' />";
            var action_pause = "<img alt='Pause' title='Pause' src='icon_pause.png' />";
            var action_cancel = "<img alt='Cancel' title='Cancel' src='icon_cancel.png' />";
            var action_remove = "<img alt='Remove' title='Remove' src='icon_remove.png' />";
            var action_play_or_pause = (status === "paused") ? action_play : action_pause;

            var single_download = $("<tr/>");
                items.push("<td class='download_data'>");
                    items.push("<div class='download_name'>" + elem['name'] + "</div>");
                    items.push("<div class='download_actions'>");
                        if (status !== "canceled" && status !== "completed")items.push("<div class='download_cancel' onClick='cancelDownload("+index+")'>" + action_cancel + "</div>");
                        if (status === "paused" || status === "downloading") items.push("<div class='download_play_pause' onClick='toggleDownload("+index+", \""+status+"\")'>" + action_play_or_pause + "</div>");
                        if (status === "canceled" || status === "completed") items.push("<div class='download_remove' onClick='removeDownload("+index+")'>" + action_remove + "</div>");
                    items.push("</div>");
                items.push("</td>");

                items.push("<td class='download_info'>");
                items.push("<div class='download_bar'><div class='download_progress' /></div>");
                var download_info = "";
                if (size == 0) {
                    percent = 100;
                    download_info = ( (status === "paused") ? "Paused - " : "Downloading, but " ) + "unknown size";
                } else if (status === "completed") {
                    percent = 100;
                    download_info = "Download completed";
                } else if (status === "canceled") {
                    percent = 0;
                    download_info = "Download canceled";
                } else {
                    percent = (progress * 100 / size).toFixed(2);
                    download_info = elem['readableProgress'] + " on " + elem['readableSize'];
                    if (status === "paused") {
                        download_info = "Paused - " + download_info;
                    } else {
                        download_info += ", speed: " + elem['readableSpeed'];
                    }
                }
                items.push("<div>" + download_info + "</div></td>");

            single_download.append(items.join(""));
            single_download.find(".download_progress").css("width", percent + "%");
            downloads_array.push(single_download);
        });
        downloads.html("");
        var thead = $("<tr />");
        thead.append("<th>Name</th><th>Progress</th>");
        downloads.append(thead);

        downloads_array.forEach(function(download) {
            downloads.append(download);
        });
        downloads.filter(':hidden').show();
        message.hide();
        if (downloads.html()==="") {
            downloads.hide();
            message.show();
            message.html("There are no active downloads. Add a link to start!");
        } else {
            adjustTable();
        }
    })
    .fail(function() {
        downloads.hide();
        message.show();
        message.html("Could not connect to the server. Please make sure your Android device is on and StreamStation is running.");
    });
}

function adjustTable() {
    var content = $('#content tr');
    content.filter(':even').addClass('even');
    content.filter(':odd').addClass('odd');
    content.filter(':last-child').css('border-bottom', '0');
}

function cancelDownload (index) {
    $.ajax( "single?action=cancel&id=" + index )
    .always(function() {
        loadJson();
    });
}

function removeDownload (index) {
    $.ajax( "single?action=remove&id=" + index )
    .always(function() {
        loadJson();
    });
}

function toggleDownload (index, status) {
    var action = (status === "paused") ? "play" : "pause";
    $.ajax( "single?action="+action+"&id=" + index )
    .always(function() {
        loadJson();
    });
}