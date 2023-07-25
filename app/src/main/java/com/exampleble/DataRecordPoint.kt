package com.exampleble

import io.realm.RealmList
import io.realm.RealmObject

open class DataRecordPoint : RealmObject() {
    open var count = 0
    open var heartSound = RealmList<Float>()
}