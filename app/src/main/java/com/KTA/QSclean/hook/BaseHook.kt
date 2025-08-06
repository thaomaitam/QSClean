package com.KTA.QSclean.hook

abstract class BaseHook {
    var isInit: Boolean = false
    abstract fun init()
}
