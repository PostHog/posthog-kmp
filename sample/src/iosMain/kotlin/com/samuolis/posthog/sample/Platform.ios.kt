package com.samuolis.posthog.sample

import platform.UIKit.UIDevice

actual fun getPlatformName(): String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
