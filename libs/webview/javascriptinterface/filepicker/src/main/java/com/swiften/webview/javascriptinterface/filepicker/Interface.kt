package com.swiften.webview.javascriptinterface.filepicker

import com.swiften.commonview.activity.IActivityResultLauncher

typealias PickFileInput = Unit

typealias PickFileOutput = FilePickerJavascriptInterface.MethodResult.PickFile

typealias FilePickerActivityResultLauncher = IActivityResultLauncher<PickFileInput, PickFileOutput?>