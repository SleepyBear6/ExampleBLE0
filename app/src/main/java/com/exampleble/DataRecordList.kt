package com.exampleble

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.Required
import org.bson.types.ObjectId
import kotlin.properties.Delegates

@RealmClass
open class DataRecordList : RealmObject(){

    open var id : Int = 0

    open var createTime = ""
    open var recordData = RealmList<DataRecordPoint>()
}