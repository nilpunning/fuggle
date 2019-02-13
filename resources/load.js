// Ensures no 'console is undefined' errors
(function() {
    var stub = function(key, fn) {
        window.console[key] = window.console[key] || fn;
    },
    keys = [
        'warn',
        'debug',
        'info',
        'error',
        'time',
        'timeEnd',
        'dir',
        'profile',
        'clear',
        'exception',
        'trace',
        'assert',
    ];

    window.console = window.console || {};
    stub('log', function() {});

    // Some browsers, ex IE 10 have console.log, but not console.debug.
    for(key in keys) {
        stub(keys[key], window.console.log);
    }

    // Only load cljs if browser supports history
    if(window.history && window.history.pushState) {
        (function() {
            var script = document.createElement('script');
            script.src = '/resources/js/app.js';
            document.write(script.outerHTML);
        })();
    }
})();