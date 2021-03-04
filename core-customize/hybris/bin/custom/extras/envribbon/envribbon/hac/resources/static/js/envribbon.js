"use strict";
(function() {
    var ribbonData = document.getElementById("mainContainer").dataset.contextpath + "/ribbon/data",
        xhr = new XMLHttpRequest();

    xhr.onload = function () {
        var container = document.createElement("div"), env;
        if (xhr.status >= 200 && xhr.status < 300) {
            env = JSON.parse(xhr.responseText);
            if (env.code && env.type) {
                container.id = 'mpern-env-ribbon';
                container.classList.add('mpern-env-ribbon');
                container.dataset.environment = env.code;
                container.dataset.type = env.type;

                if (env.aspect) {
                    container.dataset.environment += "\n(" + env.aspect + ")";
                }

                document.body.insertBefore(container, document.body.firstChild);
            }
        }
    };
    xhr.open('GET', ribbonData);
    xhr.setRequestHeader("Accept", "application/json");
    xhr.send();
})();