package com.exampleble.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.exception.BleException
import com.desarollobluetooth.fragments.ARG_PARAM1
import com.desarollobluetooth.fragments.ARG_PARAM2
import com.exampleble.DataRecordList
import com.exampleble.DataRecordPoint
import com.exampleble.MainActivity
import com.exampleble.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.yjlab.drawingpack.createLineChart
import com.yjlab.drawingpack.resetLineChart
import com.yjlab.drawingpack.updateLineChartValue
import io.realm.*
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_show_chart.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import io.reactivex.subjects.PublishSubject
import java.io.*
import android.media.AudioAttributes
import android.os.Environment.DIRECTORY_DOCUMENTS
import com.github.mikephil.charting.components.LimitLine
import com.google.android.material.math.MathUtils
import com.yjlab.SignalProcessing
import java.util.concurrent.TimeUnit
import io.reactivex.plugins.RxJavaPlugins
import java.lang.Exception
import kotlin.math.log


class ShowChartFragment:Fragment() {
    /**************BLE Parameter*************/
    private val bleUARTServiceUUID = "0783B03E-8535-B5A0-7140-A304D2495CB7"
    private val bleUARTCharacteristicUUID = "0783B03E-8535-B5A0-7140-A304D2495CB8"

    private var flagBleNotifySuccess = false
    // val detectionBlock = ConnectionDetection()
    /**************Data Plotting Parameter*************/
    private var removalCounterEMG = 0
    private var removalCounterG = 0
    private val VISIBLE_COUNT = 12000f
    private val VISIBLE_TOP = 10000F
    private val VISIBLE_BOTTOM = -10000F

    private var flagPlotting = false

    private var flagFirstTimePlotting = true
    /**************File Storage Parameter*************/
    private var fileChosen = ""
    private var deletpostion = ""

    private val realmName = "testRealm.realm"
    private val config = RealmConfiguration.Builder().name(realmName)
        .allowQueriesOnUiThread(true)
        .allowWritesOnUiThread(true)
        .build()

    private val uiThreadRealm : Realm = Realm.getInstance(config)

    val executorService: ExecutorService = Executors.newFixedThreadPool(2)

    /**************Dropdown Spinner Parameter*************/
    private lateinit var adapterSpinner : ArrayAdapter<String>
    private lateinit var listSpinner : List<String>
    /*****************************************************/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_show_chart, container, false)
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // val btnGetData = view.findViewById<Button>(R.id.btnGetData)
        // val btnStartRecord = view.findViewById<Button>(R.id.btnStartRecord)
        // val btnShowAll = view.findViewById<Button>(R.id.btnShowAll)
        // val btnPlaySound = view.findViewById<Button>(R.id.btnPlaySound)
        val lineChartEMG = view.findViewById<LineChart>(R.id.chartEMG)

        val textIntro = view.findViewById<TextView>(R.id.textIntro)
        var durationTime = view.findViewById<EditText>(R.id.showTime)
        var timerCountSecond = Timer()
        var flagBtnStartRecord = false
        val bleDevice = (activity as MainActivity).getBleDevice()
        var countRecordTime = 0
        lateinit var jobRecord:Job
        var stringRecordType = "HeartSound"
        var stringRecordName = "Heart Sound"
        var isPlaying = false
        var isPause = false
        var upSamplingArray: ArrayList<Float>
        /*
        if (bleDevice != null) {
            // synchronizeState()
            detectionBlock.view = view
            detectionBlock.setTimer(bleDevice, 1000L)
        }
        */


//        if (!flagBtnStartRecord) {
//            val olist0: ObservableList<Float> = ObservableList()
//            flagPlotting = true
//            olist0.isLocking = false
//            Log.d(TAG, "draw: 1")
//            Handler().postDelayed({
//                BleManager.getInstance().notify(
//                    bleDevice,
//                    bleUARTServiceUUID,
//                    bleUARTCharacteristicUUID,//characteristic.uuid.toString(),
//                    object : BleNotifyCallback() {
//                        override fun onNotifySuccess() {
//                            flagBleNotifySuccess = true
//                        }
//
//                        @SuppressLint("SetTextI18n")
//                        override fun onNotifyFailure(exception: BleException) {
//
//                        }
//
//                        // The place from where get the data
//                        // BLE收到資料時就會更新一次
//                        override fun onCharacteristicChanged(data: ByteArray) {
//                            if (flagPlotting) {
//
//                                val uData = data
//                                val arrayTmpForPlot = arrayListOf<Float>()
//
//                                /*
//                            val str = data.joinToString(",") { "%02x".format(it) }
//                            Log.d("O", str)
//                             */
//
//                                for (ii in uData.indices step 2) {
//
//                                    arrayTmpForPlot.add((uData[ii].toInt() * 256 + uData[ii + 1].toInt()).toFloat())
//                                }
//                                Log.d(TAG, "draw: " + arrayTmpForPlot)
//                                //Log.d(TAG, "onCharacteristicChanged: "+arrayTmpForPlot)
//                                olist0.add(arrayTmpForPlot)
//                            }
//                        }
//                    }
//                )
//            }, 50)
//            //plotEMG.lineChart = lineChartEMG
//            // plotEMG.resetLineChart()
//            resetLineChart(lineChartEMG)
//            //plotEMG.VISIBLE_COUNT = VISIBLE_COUNT
//            val filter = SignalProcessing()
//            val entriesECG1 = arrayListOf<Entry>()
//            val dataSetECG1 = LineDataSet(entriesECG1, stringRecordName)
//            createLineChart(lineChartEMG, VISIBLE_COUNT, *arrayOf(dataSetECG1))
//            // plotEMG.createLineChart(*arrayOf(dataSetECG1))
//            val yAxisRight = lineChartEMG.axisRight
//            yAxisRight.isEnabled = false
//            val yAxisLeft = lineChartEMG.axisLeft
//            yAxisLeft.axisMaximum = VISIBLE_TOP
//            yAxisLeft.axisMinimum = VISIBLE_BOTTOM
//            // [optional] original to another plot
//            yAxisLeft.granularity = 1f
//            //lineChartEMG.setVisibleXRangeMaximum(24001F)
//            olist0.getObservable().subscribe {
//                if (!olist0.isLocking) {
//                    val queuePlottingECG1 = ArrayList<Float?>()
//                    olist0.isLocking = true
//                    runOnUiThread {
//                        Log.d(TAG, "draw10: on")
//                        // Log.d("test", it.toString())
//                        val iterData =
//                            it.iterator()//繼承queuePlottingECG1，所以是ArrayList<Float>
//                        while (iterData.hasNext()) {
//                            val dataFilted = filter.signalFilter(iterData.next(), 0)//
//                            queuePlottingECG1.add(dataFilted)
//                            //Log.d(TAG, "onViewCreated:123: "+queuePlottingECG1)
//                        }
//                        if (queuePlottingECG1.isNotEmpty()) {
//                            updateLineChartValue(
//                                lineChartEMG,//Linechart
//                                VISIBLE_COUNT,
//                                queuePlottingECG1
//                            )
//
//                            // plotEMG.updateLineChartValue(*arrayOf(queuePlottingECG1))
//                        }
//                    }
//                    Log.d(TAG, "olist0: on")
//
//                }
//            }
//            jobRecord = lifecycleScope.launch(Dispatchers.Default) {
//                //*畫圖*/
//                Log.d(TAG, "draw2: on")
//                val two = async {
//
//                }
//                two.await()
//            }
//            Log.d("Size", bufferSize.toString())
//        }

        /*********************************
         * Spinner Initializer
         *********************************/
        uiThreadRealm.executeTransaction {
            listSpinner = it
                .where(DataRecordList::class.java).findAll()
                .stream()
                .map(DataRecordList::createTime).collect(Collectors.toList())
        }

        adapterSpinner = ArrayAdapter(
            activity as MainActivity, android.R.layout.simple_spinner_dropdown_item, listSpinner)
        spinnerList.adapter = adapterSpinner

        /**
        設定spinner的預設選項
         */
        var lastpostion = listSpinner.size
        spinnerList.setSelection(lastpostion-1, false)

        /*********************************
         * Spinner Selection Listener
         *********************************/
        spinnerList.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                fileChosen = parent.getItemAtPosition(pos) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        /**刪資料*/
        btnClearData.setOnClickListener{
            //val fileName = "/storage/emulated/0//HARCSV/1.txt"
            //val f = File( fileName)
            //Log.d(TAG, "onViewCreated: "+f)

            //打开文件
            //val content = File(fileName).readText(Charsets.UTF_8)
            //Log.d(TAG, "onViewCreated: "+content)

            AlertDialog.Builder(this.activity)
                .setTitle("刪除資料")
                .setMessage("確定刪除?")
                .setPositiveButton("否",null)
                .setNeutralButton("是") {_, _ ->Handler().postDelayed({
                // Get list from the database to the dropdown spinner
                uiThreadRealm.beginTransaction()
                uiThreadRealm.deleteAll()
                listSpinner  = emptyList()
                uiThreadRealm.commitTransaction()
                adapterSpinner = ArrayAdapter(this.requireActivity(), android.R.layout.simple_spinner_dropdown_item, listSpinner)
                spinnerList.adapter = adapterSpinner
                }, 300)}
                .show()
        }



        /*********************************
         * Button
         * Choose Record Place
         *********************************/
        rBtnGroupRecordPlace.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rBtnLungSound -> stringRecordType = "LungSound"
                R.id.rBtnLungSound -> stringRecordName = "Lung Sound"
                //R.id.rBtnPlaceP -> stringRecordPlace = "P"
                //R.id.rBtnPlaceA -> stringRecordPlace = "A"
                //R.id.rBtnPlaceE -> stringRecordPlace = "E"
                R.id.rBtnHeartSound -> stringRecordType = "HeartSound"
                R.id.rBtnHeartSound -> stringRecordName = "Heart Sound"
            }
        }

        /*********************************
         * Button
         * Play the sound of recorded Sound (in raw)
         *********************************/
        lateinit var alertDialog: AlertDialog
        val alertBuilder = AlertDialog.Builder(this.requireActivity())

        val bufferSound = arrayListOf<Float>()
        val filter = SignalProcessing()
        val sampleRate = 6400// 48000 //6400
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val frame_out: ShortArray = ShortArray(bufferSize)//
        val frame_out2 = arrayOf<Int>()
        val player = AudioTrack.Builder()
            //屬性
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            //格式
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            //設置讀取音訊的緩衝區大小
            .setBufferSizeInBytes(bufferSize)
            .build()

        btnPlaySound.setOnClickListener {
            if (!isPlaying) {
                val xAxis = lineChartEMG.xAxis //X軸
                xAxis.granularity = 1f
                isPlaying = true
                isPause = false
                Log.d(TAG, "onViewCreated: play1")

                //讀取儲存在datarecord裡的資料
                uiThreadRealm.beginTransaction()
                //讀取手機內部資料(測試用的)
                /*val fileName = "/storage/emulated/0//HARCSV/M_2022-11-11-15-50-27.txt"
                val f = File(fileName)
                Log.d(TAG, "onViewCreated: " + f)
                //打开文件
                val String = f.readText()
                val latestData = String.split(",").toTypedArray()*/
                val latestData = uiThreadRealm
                    .where(DataRecordList::class.java)
                    .contains("createTime", fileChosen)
                    .findAll().last()?.recordData
                uiThreadRealm.commitTransaction()
                upSamplingArray = upsample(latestData)
                upSamplingArray = (upSamplingArray - upSamplingArray.average()) as ArrayList<Float>
                var count3 = 0F
                var count = 0
                val date1 = Date()
                val task1: FutureTask<String> = FutureTask({
                    player.play()
                    /*btnSoundpause.setOnClickListener {
                        if (isPlaying && !isPause) {
                            player.pause()
                            isPause = true
                        }else{
                            player.play()
                            isPause = false
                        }
                    }*/
                    while (isPlaying) {
                        Log.d(TAG, "onViewCreated: ON")
                        for (i in 0 until bufferSize) {
                            count += 1

                            if (count >= upSamplingArray.size - 3200) {
                                break
                            }
                            if (upSamplingArray[count].toInt() > 32767) {
                                upSamplingArray[count]= 0F
                            }
                            if (upSamplingArray[count].toInt() < -32768) {
                                upSamplingArray[count] = 0F
                            }
                            frame_out[i] = (upSamplingArray[count].toInt()*4).toShort()
                            /*frame_out[i] = ((upSamplingArray[count]/64).toInt().toShort()*128).toShort()
                            if (frame_out[i] > 30000) {
                                frame_out[i] = 0
                            }
                            if (frame_out[i] < -30000) {
                                frame_out[i] = 0
                            }*/
                            //frame_out2[i] = upSamplingArray[count].toInt()

                            //Log.d("test", "count1234:"+frame_out2[i])
                        }
                        Log.d("test", "count123:"+frame_out[100])
                        Log.d("test", "count1234:"+upSamplingArray.average())
                        //Log.d(TAG, "Sound1: +play")
                        //val str = frame_out.joinToString(",") { "%02x".format(it) }
                        //Log.d("Sound", str)
                        if (count >= upSamplingArray.size - 3200) {
                            Log.d("test", "END Playing!")
                            isPlaying = false
                            lineChartEMG.moveViewToX((0f))
                            break
                        }
                        count3 += (bufferSize/2.63).toFloat()
                        lineChartEMG.moveViewToX((count3))
                        player.write(frame_out,0,bufferSize)
                    }
                }, "test")
                executorService.execute(task1)
                /*val task2: FutureTask<String> = FutureTask({
                    var ll1 = LimitLine(count3)
                    while (isPlaying) {
                        var edgeLimit =0F
                        if (durationTime.text.toString().length !=0){
                            edgeLimit = durationTime.text.toString().toFloat()
                        }else{
                            edgeLimit = 10000F
                        }
                        count3 += 0.5F
                        var count4 = 1
                        /*ll1 = LimitLine(count3)
                        xAxis.removeAllLimitLines() //先清除原来的线，后面再加上，防止add方法重复绘制
                        xAxis.addLimitLine(ll1)
                        lineChartEMG.invalidate()
                        TimeUnit.MICROSECONDS.sleep(100)*/

                        /*if (count3 >= edgeLimit * (count4)) {
                            count4 += 1
                            lineChartEMG.moveViewToX((edgeLimit * count4))
                            Log.d(TAG, "onViewCreated: "+count4)
                        }*/
                        //count2 += 1
                    }
                    /*ll1 = LimitLine(0F)
                    xAxis.removeAllLimitLines() //先清除原来的线，后面再加上，防止add方法重复绘制
                    xAxis.addLimitLine(ll1)
                    lineChartEMG.invalidate()
                    lineChartEMG.moveViewToX(0F)
                    val date2 = Date()
                    //isPlaying = false
                    System.out.println("----程式結束執行----，程式執行時間【" + (date2.getTime() - date1.getTime()) + "毫秒】")*/
                }, "test")

                //executorService.execute(task2)

                var count2 = 0
                var count3 = 0F
                val task: FutureTask<String> = FutureTask({
                    while (isPlaying) {
                        for (i in 0 until bufferSize) {
                            count += 1
                            if (count >= upSamplingArray.size - 2000) {
                                break
                            }
                            frame_out[i] = upSamplingArray[count].toInt().toShort()
                        }
                        //Log.d(TAG, "Sound1: +play")
                        //val str = frame_out.joinToString(",") { "%02x".format(it) }
                        //Log.d("Sound", str)
                        if (count >= upSamplingArray.size - 2000) {
                            Log.d("test", "END Playing!")
                            isPlaying = false
                            break
                        }
                        while (count3 < (count2 + 1) * bufferSize) {
                            count3 += 10F
                            val ll1 = LimitLine(count3)
                            xAxis.removeAllLimitLines() //先清除原来的线，后面再加上，防止add方法重复绘制
                            xAxis.addLimitLine(ll1)
                            lineChartEMG.invalidate()
                            Log.d(TAG, "onViewCreated: " + count3)
                        }
                        count2 += 1
                        player.write(frame_out, 0, bufferSize)
                        player.play()
                        lineChartEMG.moveViewToX((count2 * bufferSize - 2000).toFloat())
                    }
                    player.release()
                    Log.d(TAG, "onViewCreated: " + 123)
                    val ll1 = LimitLine(0F)
                    xAxis.removeAllLimitLines() //先清除原来的线，后面再加上，防止add方法重复绘制
                    xAxis.addLimitLine(ll1)
                    lineChartEMG.invalidate()
                }, "test")
                executorService.execute(task)*/
            } else {
                //player.stop()
                isPlaying = false
            }

            }
        /*********************************
         * Button
         * Used for getting data from the database and writing it to a CSV file
         *********************************/
        btnGetData.setOnClickListener {
            var sound_name = ""
            if (!fileChosen.equals("")) {
                sound_name = fileChosen.substring(0,5)
                if (sound_name.equals("Heart")){
                    sound_name = "HeartSound"
                }else{
                    sound_name = "LungSound"
                }
            }else{
                sound_name = "HeartSound"
            }
            val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).path+ "/heartsound_lungSound_rocording/"+sound_name

            Log.d(TAG, "123: "+dir)
            //val dir = getExternalStoragePrivateDir()
            //Log.d(TAG, "onViewCreated: "+dir)
            Log.d("test", fileChosen)
            val fileName = fileChosen + ".csv"
            var f = File(dir)
            if (f.mkdirs()) {
                System.out.println("新增資料夾");
            } else {
                System.out.println("資料夾已存在");
            }
            f.createNewFile()
            Log.d(TAG, "123: "+f.exists())
            f = File(dir, fileName)
            try {
                val outputStream = FileOutputStream(f)
                uiThreadRealm.executeTransaction {
                    val latestData = it
                        .where(DataRecordList::class.java)
                        .contains("createTime", fileChosen)
                        .findAll().last()?.recordData

                    for (num in 0 until latestData?.size!!) {
                        val dataUByteWriteIn = latestData[num]?.heartSound
                        for (len in 0 until dataUByteWriteIn!!.size) {
                            val value = dataUByteWriteIn[len]
                            outputStream.write((value.toString()).toByteArray())
                            outputStream.write(",\n".toByteArray())
                        }
                    }
                }
                outputStream.close()
                runOnUiThread {
                    textIntro.text = "寫檔成功"
                }
            } catch (e : FileNotFoundException) {
                runOnUiThread {
                    textIntro.text = e.toString()
                }
            }
        }

        /*********************************
         * Button
         * Use to Show All the data after measuring
         *********************************/
        btnShowAll.setOnClickListener {
            val listEntries = ArrayList<Entry?>()
            val filter = SignalProcessing()

            resetLineChart(lineChartEMG)

            uiThreadRealm.executeTransaction { realm ->
                val latestData = realm
                    .where(DataRecordList::class.java)
                    .contains("createTime", fileChosen)
                    .findAll().last()?.recordData

                val dataList = latestData?.stream()?.map(DataRecordPoint::heartSound)
                //Log.d("test", dataList.toString())

                for (num in 0 until latestData?.size!!) {
                    val dataUByteWriteIn = latestData[num]?.heartSound

                    for (len in 0 until dataUByteWriteIn!!.size) {
                        val value = filter.signalFilter(dataUByteWriteIn[len]!!.toFloat(), 0)
                        listEntries.add(Entry((num*dataUByteWriteIn.size + len).toFloat(), value))
                    }
                }
                Log.d("test", listEntries.toString())

                runOnUiThread {
                    val dataSetECG1 = LineDataSet(listEntries, stringRecordName)//圖表的資料
                    createLineChart(lineChartEMG, VISIBLE_COUNT, *arrayOf(dataSetECG1))//建立圖表
                    val yAxisRight = lineChartEMG.axisRight//右側Y軸，不顯示
                    yAxisRight.isEnabled = false
                    val yAxisLeft = lineChartEMG.axisLeft//左側Y軸，單位為1F
                    yAxisLeft.granularity = 1f
                    dataSetECG1.isHighlightEnabled = false
                    //drawGridLine(c, positions[i], positions[i + 1], gridLinePath);
                    // yAxisLeft.axisMinimum = 0F
                    yAxisLeft.axisMaximum = VISIBLE_TOP
                    yAxisLeft.axisMinimum = VISIBLE_BOTTOM
                    // [optional] original to another plot
                    yAxisLeft.granularity = 1f
                    //lineChartEMG.setVisibleXRangeMinimum(1000F)
                    lineChartEMG.setScaleEnabled(true)
                    val xAxis = lineChartEMG.xAxis //X軸
                    xAxis.granularity = 1f
                    var ll1 = LimitLine(0F)
                    //xAxis.addLimitLine(ll1)
                    /*if (durationTime.text.toString().length !=0){
                        lineChartEMG.setVisibleXRangeMaximum(durationTime.text.toString().toFloat())
                    }else{
                        lineChartEMG.setVisibleXRangeMaximum(12000F)
                    }*/
                    //lineChartEMG.animateX(3000)
                }

                /*
                for ((i, value) in latestData?.withIndex()!!) {
                    listEntries.add(value?.let { Entry(i.toFloat(), it.toFloat()) })
                }

                val dataSetECG1 = LineDataSet(listEntries, "ECG1 value")
                createLineChart(lineChartEMG, *arrayOf(dataSetECG1))

                val yAxisRight = lineChartEMG.axisRight
                yAxisRight.isEnabled = false
                val yAxisLeft = lineChartEMG.axisLeft
                yAxisLeft.granularity = 1f
                // yAxisLeft.axisMinimum = 0F
                // yAxisLeft.axisMaximum = 32768F
                // [optional] original to another plot
                yAxisLeft.granularity = 1f

                 */
            }
        }


        /*********************************
         * Button
         * Used for Starting recording on the device
         *********************************/
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        var localtime = ""
        btnStartRecord.setOnClickListener {

            if (!flagBtnStartRecord) {
                flagFirstTimePlotting = true
                flagBtnStartRecord = true
                flagPlotting = true
                removalCounterEMG = 0
                removalCounterG = 0
                localtime = LocalDateTime.now().format(formatter)
                val olist: ObservableList<Float> = ObservableList()

                countRecordTime = 0
                btnGetData.isEnabled = false
                //建立一個新的存檔
                uiThreadRealm.executeTransaction {
                    val tmpObj = it.createObject(DataRecordList::class.java)
                    tmpObj.createTime = stringRecordType + "_"+LocalDateTime.now().format(formatter)
                    // cast from Long to Int, or use 1 as initial value
                    tmpObj.id = it.where<DataRecordList>().max("id")?.toInt()?.plus(1) ?: 1
                }
                // Delay a little time for waiting for creating the database Object
                Handler().postDelayed({
                    BleManager.getInstance().notify(
                        bleDevice,
                        bleUARTServiceUUID,
                        bleUARTCharacteristicUUID,//characteristic.uuid.toString(),
                        object : BleNotifyCallback() {
                            override fun onNotifySuccess() {
                                flagBleNotifySuccess = true
                            }

                            @SuppressLint("SetTextI18n")
                            override fun onNotifyFailure(exception: BleException) {
                                runOnUiThread {
                                    textIntro.text = "Notify $exception"
                                }
                            }

                            // The place from where get the data
                            // BLE收到資料時就會更新一次
                            override fun onCharacteristicChanged(data: ByteArray) {
                                if (flagPlotting) {

                                    val uData = data.toUByteArray()
                                    val arrayTmpForPlot = arrayListOf<Float>()

                                    /*
                                    val str = data.joinToString(",") { "%02x".format(it) }
                                    Log.d("O", str)
                                     */

                                    for (ii in uData.indices step 2) {

                                        arrayTmpForPlot.add((uData[ii].toInt() * 256 + uData[ii + 1].toInt()).toFloat())
                                    }
                                    //Log.d(TAG, "onCharacteristicChanged: "+arrayTmpForPlot)
                                    olist.add(arrayTmpForPlot)

                                    // Write data in background Thread
                                    val task : FutureTask<String> = FutureTask(Runnable {
                                        val config = RealmConfiguration.Builder().name(realmName)
                                            .build()
                                        val backgroundThreadRealm : Realm = Realm.getInstance(config)

                                        backgroundThreadRealm.executeTransaction { bgRealm ->
                                            val lastList =
                                                bgRealm
                                                    .where(DataRecordList::class.java)
                                                    .findAll().last()
                                                    ?.recordData

                                            /*****************************
                                             * 2021/09/02
                                             * Too much time to use for-loop here,
                                             * which would take about 20ms preparing the data.
                                             * I change it to writing the binary data here directly.
                                             * And it will only take about 2ms.
                                             *****************************/
                                            /*
                                            for (ii in data.indices step 2) {
                                                val tmpObj = DataRecordPoint()
                                                // tmpObj.count = countDataPoint++
                                                tmpObj.heartSound = (uData[ii] * 256u + uData[ii + 1]).toInt()
                                                lastList?.add(tmpObj)
                                            }
                                            */
                                            val tmpObj = DataRecordPoint()
                                            for (ii in uData.indices step 2) {
                                                tmpObj.heartSound.add((uData[ii].toInt() * 256 + uData[ii + 1].toInt()).toFloat())
                                            }
                                            lastList?.add(tmpObj)
                                        }
                                        backgroundThreadRealm.close()

                                    }, "test")
                                    executorService.execute(task)//結束這次運行
                                }
                            }
                        }
                    )
                }, 50)
                jobRecord = lifecycleScope.launch(Dispatchers.Main) {
                    val one = async {
                        if (flagBleNotifySuccess) {
                            timerCountSecond = fixedRateTimer("", true, 200, 1000) {
                                runOnUiThread {
                                    textSecond.text = "量測時間 : $countRecordTime 秒"
                                }
                                countRecordTime++
                            }
                        }
                    }
                    //*畫圖*/
                    val two = async {
                        //plotEMG.lineChart = lineChartEMG
                        // plotEMG.resetLineChart()
                        resetLineChart(lineChartEMG)
                        //plotEMG.VISIBLE_COUNT = VISIBLE_COUNT
                        val filter = SignalProcessing()

                        val entriesECG1 = arrayListOf<Entry>()
                        val dataSetECG1 = LineDataSet(entriesECG1, stringRecordName)
                        Log.d(TAG, "onViewCreated:123: "+entriesECG1)
                        createLineChart(lineChartEMG, VISIBLE_COUNT, *arrayOf(dataSetECG1))
                        // plotEMG.createLineChart(*arrayOf(dataSetECG1))
                        val yAxisRight = lineChartEMG.axisRight
                        yAxisRight.isEnabled = false
                        val yAxisLeft = lineChartEMG.axisLeft
                        yAxisLeft.axisMaximum = VISIBLE_TOP
                        yAxisLeft.axisMinimum = VISIBLE_BOTTOM
                        // [optional] original to another plot
                        yAxisLeft.granularity = 1f
                        lineChartEMG.setVisibleXRangeMinimum(1000F)
                        lineChartEMG.setScaleEnabled(true)
                        if (flagPlotting) {
                            olist.getObservable().subscribe{
                            if (!olist.isLocking) {
                                val queuePlottingECG1 = ArrayList<Float?>()
                                olist.isLocking = true
                                runOnUiThread {

                                    // Log.d("test", it.toString())
                                    val iterData = it.iterator()//繼承queuePlottingECG1，所以是ArrayList<Float>
                                    while (iterData.hasNext()) {
                                        val dataFilted = filter.signalFilter(iterData.next(), 0)//
                                        queuePlottingECG1.add(dataFilted)
                                        //Log.d(TAG, "onViewCreated:123: "+iterData.next())
                                    }
                                    if (queuePlottingECG1.isNotEmpty()) {
                                        updateLineChartValue(
                                            lineChartEMG,//Linechart
                                            VISIBLE_COUNT,
                                            queuePlottingECG1
                                        )

                                        // plotEMG.updateLineChartValue(*arrayOf(queuePlottingECG1))
                                    }
                                }
                                olist.isLocking = false
                            }
                        }
                        }
                        runOnUiThread {
                            textIntro.text = "機器量測中"
                            btnStartRecord.text = "結束紀錄"
                        }
                    }
                    one.await()
                    two.await()
                }
            }
            else {
                flagBtnStartRecord = false
                flagPlotting = false
                btnGetData.isEnabled = true

                // stop every work for
                timerCountSecond.cancel()
                timerCountSecond.purge()
                jobRecord.cancel()

                // delay a little time for both BLE device closing and database writing
                Handler().postDelayed({
                    // Stop notify
                    BleManager.getInstance().stopNotify(
                        bleDevice,
                        bleUARTServiceUUID,
                        bleUARTCharacteristicUUID
                    )
                    runOnUiThread {
                        textIntro.text = "量測結束"
                        btnStartRecord.text = "開始紀錄"
                        // 按鈕觸發顯示對話方塊

                    }
                    /*val options = arrayOf("選項一", "選項二", "選項三")
                    var option = "選項一"

                    alertBuilder
                        .setTitle("標題")
                        .setSingleChoiceItems(options,-1) { _, which  ->
                            option = when (which){
                                0 -> "選項一"
                                1 -> "選項二"
                                2 -> "選項三"
                                else -> "選項一"
                            }
                        }
                        .setPositiveButton("確定") { _, _ ->*/
                            //val dir = getExternalStoragePrivateDir()
//                            val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).path+ "/heartsound_lungSound_rocording/"+stringRecordType
//                            Log.d(TAG, "123: "+dir)
//                            //val dir = getExternalStoragePrivateDir()
//                            //Log.d(TAG, "onViewCreated: "+dir)
//                            Log.d("test", fileChosen)
//                            val fileName =  stringRecordType + "_" + localtime + ".csv"
//                            var f = File(dir)
//                            if (f.mkdirs()) {
//                                System.out.println("新增資料夾");
//                            } else {
//                                System.out.println("資料夾已存在");
//                            }
//                            f.createNewFile()
//                            Log.d(TAG, "123: "+f.exists())
//                            f = File(dir, fileName)
//                            //val dir = Environment.getExternalStorageDirectory().absolutePath + "/BlogExport20"
//                            Log.d(TAG, "123: "+dir)
//                            Log.d("test", fileChosen)
//                            //val fileName =  stringRecordType + "_" + fileChosen + ".csv" //電子聽診器，heartsound_lungSound_rocording/
//                            //val f = File(dir, fileName)
//                            //try {
//                            val outputStream = FileOutputStream(f)
                            uiThreadRealm.beginTransaction()
                            listSpinner = uiThreadRealm
                                .where(DataRecordList::class.java).findAllAsync().stream()
                                .map(DataRecordList::createTime).collect(Collectors.toList())
                            uiThreadRealm.commitTransaction()

                            adapterSpinner = ArrayAdapter(this.requireActivity(), android.R.layout.simple_spinner_dropdown_item, listSpinner)
                            spinnerList.adapter = adapterSpinner
                            // Log.d("", listSpinner.toString())
                            uiThreadRealm.executeTransaction {
//                                val latestData = it
//                                    .where(DataRecordList::class.java)
//                                    .contains("createTime", fileChosen)
//                                    .findAll().last()?.recordData
//                                for (num in 0 until latestData?.size!!) {
//                                    val dataUByteWriteIn = latestData[num]?.heartSound
//                                    for (len in 0 until dataUByteWriteIn!!.size) {
//                                        val value = dataUByteWriteIn[len]
//                                        outputStream.write((value.toString()).toByteArray())
//                                        outputStream.write(",\n".toByteArray())
//                                    }
//                                }
//                                //}
//                                outputStream.close()
//                                runOnUiThread {
//                                    textIntro.text = "寫檔成功"
//                                }

                            }
                            lastpostion = listSpinner.size
                            spinnerList.setSelection(lastpostion-1, false)
                        //}
                    // 建立對話方塊
                    /*alertDialog = alertBuilder.create()
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.show()*/
                    // Get list from the database to the dropdown spinner

                }, 50)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // detectionBlock.cancelTimer()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun synchronizeState() {
        //cannot be initialized in the scope of global because the fragment wasn't belong to any activity at that time
        val bleDevice = (activity as MainActivity).getBleDevice()

        val formatter = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss")
        val timeParts = LocalDateTime.now().format(formatter).toString().split(" ")
        val timeByte = ByteArray(7)
        timeByte[0] = 0x01  //Command sent to the device for setting Time
        timeByte[1] = timeParts[0].substring(2).toByte()
        for (i in 2..6) {
            timeByte[i] = timeParts[i - 1].toByte()
        }

        //Send time to device
        BleManager.getInstance().write(
            bleDevice,
            bleUARTServiceUUID,
            bleUARTCharacteristicUUID,
            timeByte,
            object : BleWriteCallback() {
                override fun onWriteSuccess(
                    current: Int,
                    total: Int,
                    justWrite: ByteArray
                ) {
                }

                override fun onWriteFailure(exception: BleException) {}
            }
        )
    }

    open class ObservableList<list> {
        protected val list: ArrayList<Float> = ArrayList()
        private val onAdd: PublishSubject<ArrayList<Float>> = PublishSubject.create()
        var isLocking : Boolean = false
        fun add(value: ArrayList<Float>) {
            for (listA in value) {
                list += listA
            }
            onAdd.onNext(value)
        }

        open fun getObservable(): PublishSubject<ArrayList<Float>> {
            return onAdd
        }
    }

    /*
    class ConnectionDetection {
        lateinit var bleDevice : BleDevice
        var timer : Timer? = null
        var period = 0L
        var view : View? = null

        fun setTimer(bleDevice : BleDevice, period: Long) {
            this.bleDevice = bleDevice
            this.period = period

            this.timer = fixedRateTimer("", true, 0, period) {
                val mainHandler = Handler(Looper.getMainLooper());
                val myRunnable = Runnable {
                    val btnConnectionState = view?.findViewById<Button>(R.id.btnConnectionState)
                    btnConnectionState?.isEnabled = false
                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        btnConnectionState?.setBackgroundColor(Color.GREEN)
                    } else {
                        btnConnectionState?.setBackgroundColor(Color.RED)
                    }
                }
                mainHandler.post(myRunnable);
            }
        }

        fun cancelTimer() {
            this.timer?.cancel()
            this.timer?.purge()
        }
    }
    */

    private fun runOnUiThread(runnable: () -> Unit) {
        if (isAdded && activity != null)
            requireActivity().runOnUiThread(runnable)
    }

    private fun getExternalStoragePrivateDir(): File {
        val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "z2b")
        if (!file.mkdirs()) {
            Log.e("", "Directory not created or exist")
        }
        return file
    }

    @ExperimentalUnsignedTypes
    private fun checkUnsignedPoint(point: UInt) : Int {
        return if (point.toInt() > 32768) {
            point.toInt() - 65536
        } else {
            point.toInt()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ShowChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun upsample(latestData : RealmList<DataRecordPoint>?): ArrayList<Float> {//Array<String>
        val bufferSound = arrayListOf<Float>()
        val filter = SignalProcessing()
        //將濾波後的結果加進arraylist中
        /**/
        //

        /*for (ii in 0 until bufferSound.size) {
            upSamplingArray.add(filter.upSamplingFilter(bufferSound[ii])*2)
            upSamplingArray.add(filter.upSamplingFilter(0F))
        }*/
        Log.d(TAG, "upsample:1 On")
        if (latestData != null) {
            for (num in 0 until latestData.size - 1) {
                val dataUByteWriteIn = latestData[num]?.heartSound
                /*bufferSound.add(
                    filter.signalFilter2(
                        dataUByteWriteIn.toString().toFloat(),
                        0
                    )
                )*/
                for (len in 0 until dataUByteWriteIn!!.size) {//
                bufferSound.add(dataUByteWriteIn[len]!!.toFloat())//, 0)filter.signalFilter()
                //濾波
                // bufferSoundRealm.add(filter.signalFilter(dataUByteWriteIn[len]!!.toFloat(),0))
                //bufferSound.add(dataUByteWriteIn[len]!!.toFloat())
                //Log.d(TAG, "round: "+element!!.toFloat()) }
                }
            }

        }
        Log.d(TAG, "upsample:2 On")
        val louder = 1
        val upSamplingArray: ArrayList<Float> = arrayListOf()
        //將濾波過的訊號upsample
        for (ii in 0 until bufferSound.size-1) {

            upSamplingArray.add(
                bufferSound[ii]*louder
            )
            upSamplingArray.add(
                MathUtils.lerp(
                    bufferSound[ii]*louder,
                    bufferSound[ii + 1]*louder,
                    1 / 2F
                )
            )
            /*upSamplingArray.add(
                MathUtils.lerp(
                    bufferSound[ii]*louder,
                    bufferSound[ii + 1]*louder,
                    2 / 3F
                )
            )*/
            /*upSamplingArray.add(
                bufferSound[ii + 1]
            )*/
            //Log.d(TAG, "upsample: "+upSamplingArray.size)
            //upSamplingArray.add(filter.resample(0F,0))
        }
        /*upSamplingArray.add(
           bufferSound[bufferSound.size]
        )*/

        return upSamplingArray
    }
}
