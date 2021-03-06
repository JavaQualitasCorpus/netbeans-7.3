/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.NetBeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

// Initialization/cleanup
NetBeans.cleanup();

// Notify IDE that the extension has been installed/updated
chrome.runtime.onInstalled.addListener(function() {
    var manifest = chrome.runtime.getManifest();
    var version = manifest.version;
    NetBeans.sendReadyMessage(version);
});

// Register reload-callback
NetBeans.browserReloadCallback = function(tabId, newUrl) {
    if (newUrl !== undefined) {
        chrome.tabs.get(tabId, function(tab) {
            if (tab.url === newUrl) {
                chrome.tabs.reload(tabId, {bypassCache: true});
            } else {
                chrome.tabs.update(tabId, {url: newUrl});
            }
        });
    } else {
        chrome.tabs.reload(tabId, {bypassCache: true});
    }
};

NetBeans.browserCloseCallback = function(tabId) {
    chrome.tabs.remove(tabId);
};

NetBeans.debuggedTab = null;
NetBeans.windowWithDebuggedTab = null;
NetBeans.browserAttachDebugger = function(tabId) {
    if (NetBeans.DEBUG) {
        console.log('debugger attach for tab ' + tabId);
    }
    chrome.debugger.attach({tabId : tabId}, "1.0", function(){
        if (chrome.extension.lastError) {
            console.log('debugger attach result code: ' + chrome.extension.lastError);
        } else {
            NetBeans.debuggedTab = tabId;
            chrome.tabs.get(tabId, function(tab) {
                NetBeans.windowWithDebuggedTab = tab.windowId;
            });
            // detect viewport
            NetBeans.detectViewPort();
        }
    });
};

NetBeans.browserDetachDebugger = function(tabId) {
    if (NetBeans.DEBUG) {
        console.log('debugger detaching from tab ' + tabId);
    }
    chrome.debugger.detach({tabId : tabId});
    NetBeans.debuggedTab = null;
    NetBeans.windowWithDebuggedTab = null;
};

// display NB icon in URL bar
NetBeans.showPageIcon = function(tabId) {
    chrome.pageAction.show(tabId);
};
// hide NB icon in URL bar
NetBeans.hidePageIcon = function(tabId) {
    chrome.pageAction.hide(tabId);
};

// Creates the Select Mode context menu
NetBeans.createContextMenu = function(tabId, url) {
    var baseUrl = function(url) {
        // Remove anchor
        var index = url.indexOf('#');
        if (index !== -1) {
            url = url.substr(0, index);
        }
        return url;
    };
    NetBeans.contextMenuUrl = baseUrl(url);
    if (NetBeans.contextMenuCreationInProgress) {
        return;
    } else {
        NetBeans.contextMenuCreationInProgress = true;
    }
    // Removing possible orphaned context menus of this extension
    chrome.contextMenus.removeAll(function() {
        chrome.contextMenus.create({
            id: 'selectionMode',
            title: NetBeans.contextMenuName(),
            contexts: ['all'],
            documentUrlPatterns: [NetBeans.contextMenuUrl],
            onclick: function() {
                NetBeans.setSelectionMode(!NetBeans.getSelectionMode());
            }
        },
        function() {
            NetBeans.contextMenuCreationInProgress = false;
        });
    });
};

// Updates the Select Mode context menu
NetBeans.updateContextMenu = function() {
    chrome.contextMenus.update('selectionMode', {
        title: NetBeans.contextMenuName()
    });
};

// Returns the name of 'Select Mode' context menu
NetBeans.contextMenuName = function() {
    return chrome.i18n.getMessage(NetBeans.getSelectionMode() ? '_StopSelectMode' : '_StartSelectMode');
};

// show infobar
NetBeans.showInfoBar = function(tabId) {
    chrome.experimental.infobars.show({
        tabId : tabId,
        path: 'html/infobar.html'
    });
};
NetBeans.getWindowInfo = function(callback) {
    chrome.windows.getLastFocused(callback);
};
NetBeans.detectViewPort = function(callback) {
    if (NetBeans.debuggedTab === null) {
        console.log('No debuggedTab so bypassing the detection');
        if (callback) {
            callback();
        }
        return;
    }
    var script = 'NetBeans_ViewPort = {'
            + '    width: window.innerWidth,'
            + '    height: window.innerHeight,'
            + '    marginWidth: window.outerWidth - window.innerWidth,'
            + '    marginHeight: window.outerHeight - window.innerHeight,'
            + '    isMac: navigator.platform.toUpperCase().indexOf("MAC") !== -1'
            + '};';
    chrome.debugger.sendCommand(
        {tabId : NetBeans.debuggedTab},
        'Runtime.evaluate',
        {expression: script, returnByValue: true},
        function(result) {
            var viewport = result.result.value;
            NetBeans_ViewPort.width = viewport.width;
            NetBeans_ViewPort.height = viewport.height;
            NetBeans_ViewPort.marginWidth = viewport.marginWidth;
            NetBeans_ViewPort.marginHeight = viewport.marginHeight;
            NetBeans_ViewPort.isMac = viewport.isMac;
            if (callback) {
                callback();
            }
        }
    );
};
NetBeans.resetPageSize = function(callback) {
    chrome.windows.getLastFocused(function(win) {
        var opt = {};
        opt.state = 'maximized';
        chrome.windows.update(win.id, opt);
        if (callback) {
            callback();
        }
    });
};
NetBeans.resizePage = function(preset, callback) {
    if (preset === null) {
        this.resetPageSize(callback);
        return;
    }
    var data = NetBeans_Presets.getPreset(preset);
    if (data === null) {
        console.error('Preset [' + preset + '] not found.');
        return;
    }
    this._resizePage(data['width'], data['height'], callback);
};
// resize actual page
NetBeans._resizePage = function(width, height, callback) {
    this.detectViewPort(function() {
        width = parseInt(width);
        height = parseInt(height);
        // resize info
        var opt = {};
        opt.state = 'normal';
        opt.width = width + NetBeans_ViewPort.marginWidth;
        opt.height = height + NetBeans_ViewPort.marginHeight;
        // resize
        chrome.windows.getLastFocused(function(win) {
            chrome.windows.update(win.id, opt);
            if (callback) {
                callback();
            }
            // #218974
            if (NetBeans_ViewPort.isMac && width < 400) {
                NetBeans.openWarning('windowTooSmall', 230);
            }
        });
    });
};
// show preset customizer
NetBeans.showPresetCustomizer = function() {
    chrome.tabs.create({'url': 'html/options.html'});
};

NetBeans.browserSendCommand = function(tabId, id, method, params, callback) {
    if (NetBeans.DEBUG) {
        console.log('send ['+tabId+","+id+","+method+","+JSON.stringify(params));
    }
    chrome.debugger.sendCommand({tabId : tabId}, method, params,
        function(result) {
            if (chrome.extension.lastError) {
                console.log('debugger send command result code: ' + chrome.extension.lastError);
            } else {
                console.log('debugger send command response: ' + result);
                NetBeans.sendDebuggingResponse(tabId, {id : id, result : result});
            }
        });
};

// "fired" when presets changed
NetBeans_Presets.presetsChanged = function() {
    // no need to refresh popup, refresh only infobar(s)
    var views = chrome.extension.getViews({type: "infobar"});
    console.log('Refreshing ' + views.length +  ' infobars');
    for (var i in views) {
        var view = views[i];
        view.NetBeans_Infobar.redrawPresets();
    }
};

// Updates the context menu and the info-bar according to changes of page-inspection properties
NetBeans.addPageInspectionPropertyListener(function(event) {
    var name = event.name;
    if (name !== 'selectionMode') {
        return;
    }
    NetBeans.updateContextMenu();
    var value = event.value;
    var views = chrome.extension.getViews({type: "infobar"});
    for (var i in views) {
        var view = views[i];
        if (view.NetBeans_Infobar) {
            view.NetBeans_Infobar.setSelectionMode(value);
        }
    }
});

/**
 * Open page with warning about unexpected/incorrect debugger detach.
 * This means that the NetBeans integration will not work.
 * This warning is shown always except these cases:
 * 1. user closes NetBeans IDE
 * 2. the debugged tab is not more visible (tab or window closed)
 */
NetBeans._checkUnexpectedDetach = function(tabId) {
    var debuggedTab = NetBeans.debuggedTab;
    if (debuggedTab != tabId) {
        // not "NetBeans" tab
        return;
    }
    // 1. user closes NetBeans IDE
    //   -> this case already works out-of-the-box
    // delay the check since detach is called before tabClosed
    setTimeout(function() {
        // 2. the debugged tab is not more visible (tab or window closed)
        chrome.tabs.get(debuggedTab, function(tab) {
            if (tab !== undefined) {
                // the tab still exists
                NetBeans.openWarning('disconnectedDebugger', 390);
            }
        });
    }, 100);
};

NetBeans.openWarning = function(ident, height) {
    NetBeans_Warnings.runIfEnabled(ident, function() {
        NetBeans.detectViewPort(function() {
            var windowTitleHeight = NetBeans_ViewPort.marginHeight - 60; // try to remove the height of the location bar
            NetBeans.openPopup('html/warning.html#' + ident, 550, height + Math.max(windowTitleHeight, 0));
        });
    });
};

/**
 * Open popup window.
 * @param {string} url url to be opened
 * @param {int} width popup width, can be omitted
 * @param {type} height popup height, can be omitted
 * @returns {void}
 */
NetBeans.openPopup = function(url, width, height) {
    var options = {
        url: url,
        type: 'popup'
    };
    if (width !== undefined) {
        options['width'] = width;
    }
    if (height !== undefined) {
        options['height'] = height;
    }
    chrome.windows.create(options);
};

chrome.debugger.onEvent.addListener(function(source, method, params) {
    NetBeans.sendDebuggingResponse(source.tabId, {method : method, params : params});
});

chrome.debugger.onDetach.addListener(function(source) {
    NetBeans._checkUnexpectedDetach(source.tabId);
    NetBeans.hidePageIcon(source.tabId);
    chrome.contextMenus.removeAll();
    NetBeans.sendDebuggerDetached(source.tabId);
});

// Register tab listeners
chrome.tabs.onCreated.addListener(function(tab) {
    NetBeans.tabCreated(tab.id);
});
chrome.tabs.onUpdated.addListener(function(tabId, changeInfo, tab) {
    NetBeans.tabUpdated(tab);
});
chrome.tabs.onRemoved.addListener(function(tabId) {
    NetBeans.tabRemoved(tabId);
});

// onCreated event is not delivered for the first tab;
// As a workaround, we go through all existing tabs and consider them as new.
// onUpdated event is not delivered sometimes as well for the first tab;
// Hence, we consider also tab urls that are known already.
chrome.windows.getAll({populate: true}, function(windows) {
    for (var i=0; i<windows.length; i++) {
        var window = windows[i];
        for (var j=0; j<window.tabs.length; j++) {
            var tab = window.tabs[j];
            NetBeans.tabCreated(tab.id);
            var url = tab.url;
            if (url !== undefined && url !== null && url.length !== 0) {
                // URL of the tab is known already
                NetBeans.tabUpdated(tab);
            }
        }
    }
});

NetBeans.windowFocused = function(windowId) {
    if (NetBeans.debuggedTab !== null) {
        var active = (windowId === NetBeans.windowWithDebuggedTab);
        var script = 'if (typeof(NetBeans) === "object") { NetBeans.setWindowActive('+active+'); }';
        chrome.debugger.sendCommand(
            {tabId : NetBeans.debuggedTab},
            'Runtime.evaluate',
            {expression: script});
    }
};

chrome.windows.onFocusChanged.addListener(NetBeans.windowFocused);

chrome.tabs.onAttached.addListener(function(tabId, attachInfo) {
    if (NetBeans.debuggedTab === tabId) {
        // Debugger tab moved into a different window
        var windowId = attachInfo.newWindowId;
        NetBeans.windowWithDebuggedTab = windowId;
        NetBeans.windowFocused(windowId);
    }
});

/**
 * Warnings manager.
 */
NetBeans_Warnings = {};
/**
 * Runs the given task if the warning identified by the given ident is enabled.
 * @param {String} ident warning identifier
 * @param {function} task task to be run
 * @returns {void}
 */
NetBeans_Warnings.runIfEnabled = function(ident, task) {
    var key = NetBeans_Warnings._getKeyFor(ident, 'enabled');
    chrome.storage.sync.get(key, function(items) {
        NetBeans_Warnings._logError('get', key);
        if (items[key] !== undefined && items[key] === 'false') {
            // warning disabled
            return;
        }
        task();
    });
};
/**
 * Enable/disable the given warning.
 * @param {String} ident warning identifier
 * @param {boolean} true for enable, false to disable
 * @returns {void}
 */
NetBeans_Warnings.enable = function(ident, enabled) {
    var key = NetBeans_Warnings._getKeyFor(ident, 'enabled');
    if (enabled) {
        NetBeans_Warnings._remove(key);
    } else {
        // disable
        var data = {};
        data[key] = 'false';
        chrome.storage.sync.set(data, function() {
            NetBeans_Warnings._logError('set', key);
        });
    }
};
/**
 * Reset all warnings (all warnings dialogs will be shown again).
 * @returns {void}
 */
NetBeans_Warnings.reset = function() {
    chrome.storage.sync.get(function(items) {
        NetBeans_Warnings._logError('reset', 'none');
        var warningPrefix = NetBeans_Warnings._getKeyFor();
        for (var key in items) {
            if (key.indexOf(warningPrefix) === 0) {
                NetBeans_Warnings._remove(key);
            }
        }
    });
}
NetBeans_Warnings._getKeyFor = function(ident, key) {
    var keyName = 'warning.';
    if (ident !== undefined) {
        keyName += ident;
        if (key !== undefined) {
            keyName += '.' + key;
        }
    }
    return keyName;
};
NetBeans_Warnings._logError = function(operation, key) {
    if (chrome.runtime && chrome.runtime.lastError) {
        console.error('Local storage error ("' + operation + '" operation for "' + key + '"): ' + chrome.runtime.lastError.message);
    }
};
NetBeans_Warnings._remove = function(key) {
    // remove from local storage
    chrome.storage.sync.remove(key, function() {
        NetBeans_Warnings._logError('remove', key);
    });
};
