package com.yjlab.drawingpack

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class MPdrawingPackClass {
    var lineChart: LineChart? = null

    var VISIBLE_COUNT = 0f
    var removalCounter = 0f



    /**********************************
     * Create a Line Chart with default parameters.
     * Parameter:
     * lineChart - the drawn chart
     **********************************/
    fun createLineChart(vararg setList: LineDataSet) {

        for (set in setList) {
            set.clear()
            set.setDrawValues(false)
            set.setDrawCircles(false)
        }

        setList[0].color = Color.RED

        lineChart?.data = LineData(*setList)
        for (setIndex in setList.indices) {
            lineChart?.data?.removeEntry(0f, setIndex)
        }
    }
    /************************
     * Use to reset a lineChart.
     * @Parameter:
     * lineChart - the drawn chart
     * @Description:
     * Remember to call this function if you try to re-draw a lineChart on an used chart
     * with the situation that you are overwriting an existing position.
     ************************/
    fun resetLineChart(){
        // lineChart.clearValues()
        lineChart?.xAxis?.valueFormatter = null  //?????
        lineChart?.notifyDataSetChanged()
        lineChart?.clear()
        lineChart?.invalidate()
        removalCounter = 0f
    }

    fun updateLineChartValue(vararg newDataList: ArrayList<Float?>) {
        val data: LineData? = lineChart?.data
        val set: LineDataSet = data?.getDataSetByIndex(0) as LineDataSet

        for (setNum in 0 until data.dataSetCount) {
            val dataList = newDataList[setNum]
            for (newData in dataList) {
                data.addEntry(
                    newData?.let {
                        Entry(
                            (set.entryCount + removalCounter),  //+removalCounter
                            it
                        )
                    }, setNum
                )


                if (set.entryCount > VISIBLE_COUNT) {
                    removalCounter ++
                    data.removeEntry(removalCounter, setNum)
                    lineChart?.moveViewToX(removalCounter+1)
                }
            }
        }

        lineChart?.setVisibleXRangeMaximum(VISIBLE_COUNT)
        /*
        if (set.entryCount > VISIBLE_COUNT) {
            lineChart?.moveViewToX(removalCounter+1)
        }
         */

        // removalCounter += newDataList[0].size

        //notify data changed and plot
        data.notifyDataChanged()
        lineChart?.notifyDataSetChanged()
        lineChart?.invalidate()
    }
}