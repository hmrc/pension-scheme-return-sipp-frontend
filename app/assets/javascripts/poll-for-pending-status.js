function pollForPendingStatus(call) {
    function polling(id) {
        setTimeout(function () {
            $.ajax({
                url: call,
                type: "GET",
                success: function (data) {
                    if(data.isPending) {
                        polling(id)
                    } else {
                        window.location.assign(data.redirectUrl)
                    }
                },
                error: function (XMLHttpRequest, textStatus, errorThrown) {
                    polling(id);
                },
                dataType: "json",
                timeout: 1000
            })}, 1000)
    };

    polling(call)
}