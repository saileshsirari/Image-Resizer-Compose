package com.image.resizer.compose

import android.R.id.message
import android.util.Log
const val TAG = "LogUtils"
fun log(message: String,tag:String  = TAG, printConsole: Boolean =false){
    Log.d(tag,message)
    if(printConsole){
        println(message)
    }
}