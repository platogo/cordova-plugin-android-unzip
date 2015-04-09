module.exports = {
    unzip: function(zipPath, destPath, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            "AndroidUnzip",
            "unzip", [zipFilePath, destPath]
        );
    }
};
