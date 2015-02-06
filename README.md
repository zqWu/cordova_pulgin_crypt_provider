sample usage

```
#!/bin/bash

# create project
cordova create hello com.example.hello HelloWorld

# platform android
cd hello
cordova platform add android

# install
plugman install --project . --platform android --plugin org.wzq.android.cryptprovider
```

in Project[HelloWorld] com.example.hello.CordovaApp.java
```
		loadUrl(ProviderCrypt.getCombineURL(launchUrl));
//		loadUrl(launchUrl);
```

modify config.xml
```
    <content src="test_crypt.html" />
```
modify Project[CordovaApp-CordovaLib] org.apache.cordova.CordovaBridge.java
line 165, support content://
```
//if ( origin.startsWith("file:") || (origin.startsWith("http") && loadedUrl.startsWith(origin))) {
if (<font color="red">origin.startsWith("content://")||<font> origin.startsWith("file:") || (origin.startsWith("http") && loadedUrl.startsWith(origin))) {
```
