var exec = require('cordova/exec');

module.exports = {
    connect: function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "connect", [startInfo]);
    },
    subscribe: function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "subscribe", [startInfo]);
    },
	disconnect:function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "disconnect", [startInfo]);
    },
	unsubscribe:function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "unsubscribe", [startInfo]);
    },
    onmessage:function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "onmessage", []);
    },
    notifyclick:function(startInfo,onSuccess,onError){
        exec(onSuccess, onError, "MessageNotify", "notifyclick", []);
    }
};