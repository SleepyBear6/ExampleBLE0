package com.yjlab.drawingpack

import android.graphics.Color
import android.os.Build
import android.renderscript.Sampler
import android.util.Log
import androidx.annotation.RequiresApi
import com.exampleble.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Collectors.toList
import java.util.stream.IntStream

/**********************************
 * Create a Line Chart with default parameters.
 * Parameter:
 * lineChart - the drawn chart
 **********************************/
fun createLineChart(lineChart: LineChart,VISIBLE_COUNT : Float, vararg setList: LineDataSet) {
    for (set in setList) {
        set.setDrawCircles(false)
        set.setDrawValues(false)
        set.color = Color.BLACK
        Log.d("test", set.toString())
    }

    lineChart.setVisibleXRangeMaximum(VISIBLE_COUNT)
    lineChart.data = LineData(*setList)
    lineChart.data.notifyDataChanged()
    lineChart.notifyDataSetChanged()
    lineChart.invalidate()
}
/************************
 * Used to reset a lineChart.
 * @Parameter:
 * lineChart - the drawn chart
 * @Description:
 * Remember to call this function if you try to re-draw a lineChart on an used chart
 * with the situation that you are overwriting an existing position.
 ************************/
fun resetLineChart(lineChart: LineChart){
    // lineChart.clearValues()
    lineChart.removeAllViews()
    lineChart.data = null
    lineChart.xAxis.valueFormatter = null  //?????
    lineChart.notifyDataSetChanged()
    lineChart.clear()
    lineChart.invalidate()
}

@RequiresApi(Build.VERSION_CODES.N)
fun updateLineChartValue(lineChart: LineChart, VISIBLE_COUNT: Float, vararg newDataList: ArrayList<Float?>) {
    val data: LineData = lineChart.data
    val set: LineDataSet = data.getDataSetByIndex(0) as LineDataSet
    val dataList = newDataList[0]
    val dataSliced : MutableList<Float>
    val listEntries = ArrayList<Entry?>()

    // Change Data from Entries to List
    val dataListGenerated : MutableList<Float> = set.values.stream().map(Entry::getY).collect(toList())

    /**********************
     * 2021.08.30
     * Using removeFirst will cost too much time (up to few seconds)
     * So use sublist instead
     * ********************/
    /*
    while (dataListGenerated.size + dataList.size > VISIBLE_COUNT) {
        dataListGenerated.removeFirst()
    }
     */
    dataSliced = if (dataListGenerated.size + dataList.size > VISIBLE_COUNT) {
        dataListGenerated.subList(
            (dataListGenerated.size + dataList.size - VISIBLE_COUNT).toInt() + 1,
            dataListGenerated.size
        )
    }else {
        dataListGenerated
    }
    Log.d("123", "updateLineChartValue:1234: "+dataListGenerated.size+";"+dataList.size)
    //dataSliced = dataListGenerated
    // Concatenate two Lists
    val listConcatenated = listOf(dataSliced, dataList.stream().collect(toList())).flatten()
    // Log.d("test", listConcatenated.toString())

    // Change Data from List to Entries
    for ((i, value) in listConcatenated.withIndex()) {
        listEntries.add(value?.let { Entry(i.toFloat(), it) })
    }
    // Log.d("test", listEntries.toString())

    val setNewData = LineDataSet(listEntries, "ECG1 value")

    setNewData.setDrawHighlightIndicators(false)
    setNewData.setDrawCircles(false)
    setNewData.setDrawValues(false)
    setNewData.color = Color.BLACK
    lineChart.data = LineData(setNewData)
    lineChart.setVisibleXRangeMaximum(VISIBLE_COUNT)
    //notify data changed and plot
    data.notifyDataChanged()
    lineChart.notifyDataSetChanged()
    //lineChart.moveViewToX()
    lineChart.invalidate()
}
