# WebIntent Android Plugin for Tabris Js

## Sample code

## Using the plugin

The plugin creates the object `window.plugins.webintent` with five methods:

### startActivity

Launches an Android intent. For example:

```javascript
window.plugins.webintent.startActivity({
    action: window.plugins.webintent.ACTION_VIEW,
    url: 'geo:0,0?q=' + address},
    function () {},
    function () { alert('Failed to open URL via Android Intent'); }
);
```

### hasExtra

Checks if this app was invoked with the specified extra. For example:

```javascript
window.plugins.webintent.hasExtra(WebIntent.EXTRA_TEXT,
    function (has) {
        // `has` is true iff app invoked with specified extra
    }, function () {
        // `hasExtra` check failed
    }
);
```

### getExtra

Gets the extra that this app was invoked with. For example:

```javascript
window.plugins.webintent.getExtra(WebIntent.EXTRA_TEXT,
    function (url) {
        // `url` is the value of EXTRA_TEXT
    }, function () {
        // There was no extra supplied.
    }
);
```

### getUri

Gets the URI the app was invoked with. For example:

```javascript
window.plugins.webintent.getUri(function (uri) {
    if (uri !== '') {
        // `uri` is the uri the intent was launched with.
        //
        // If this is the first run after the app was installed via a link with an install referrer
        // (e.g. https://play.google.com/store/apps/details?id=com.example.app&referrer=referrer.com)
        // then the Play Store will have fired an INSTALL_REFERRER intent that this plugin handles,
        // and `uri` will contain the referrer value ("referrer.com" in the example above).
        // ref: https://help.tune.com/marketing-console/how-google-play-install-referrer-works/
    }
});
```

### onNewIntent

Gets called when `onNewIntent` is called for the parent activity.
Used in only certain launchModes. For example:

```javascript
window.plugins.webintent.onNewIntent(function (uri) {
    if (uri !== '') {
        // `uri` is the uri that was passed to onNewIntent
    }
});
```

### sendBroadcast
Sends a custom intent passing optional extras

```javascript
window.plugins.webintent.sendBroadcast({
    action: 'com.dummybroadcast.action.triggerthing',
    extras: { option: true }
  }, function() {
  }, function() {
});
```