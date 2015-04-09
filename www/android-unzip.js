module.exports = {
    unzip: function(zipPath, destPath, successCallback, errorCallback, progressCallback) {
        var successProxy = function(result) {
            if (result && typeof result.loaded != "undefined") {
                if (progressCallback) {
                    return progressCallback(result);
                }
            } else if (successCallback) {
                successCallback();
            }
        };

        cordova.exec(
            successProxy,
            errorCallback,
            "AndroidUnzip",
            "unzip", [zipFilePath, destPath]
        );
    }
};
