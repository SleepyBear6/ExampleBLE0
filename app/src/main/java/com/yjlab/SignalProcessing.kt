package com.yjlab

import io.realm.RealmList

class SignalProcessing {
    /***********************************
     *  Parameter for signal filtering.
     ***********************************/
    private val filterOrder = 2
    private val filterLen = filterOrder*2+1// * 2
    /* a in Matlab */
    val denomCoeff = floatArrayOf(
        1F,//1F,
        (-3.49300719131676).toFloat(), 4.61902203541957F,//(-3.74365097694118).toFloat(), 5.29209951616327F,//(-3.74365097694118).toFloat(), 5.29209951616327F,//(-3.58623980811691).toFloat(), 4.84628980428802F,
        (-2.74939828790452).toFloat(), 0.623812203501364F
        //(-0.9091).toFloat()       //(-3.34894442162641).toFloat(), 0.800802646665707F//(-2.93042721682014).toFloat(), 0.670457905953174F
    )

    /* b in Matlab */
    val numCoeff = floatArrayOf(
        0.0224061538266426F,0F, (-0.0448123076532853).toFloat(),0F,0.0224061538266426F//,(-3.70364847899422).toFloat(), 0.925912119748556F//, (-3.70364847899422).toFloat(), 0.925912119748556F//0.00554271721028071F, 0F, (-0.0110854344205614).toFloat(), 0F, 0.00554271721028071F//0.0165819316693031F, 0F, (-0.0331638633386063).toFloat(), 0F, 0.0165819316693031F//0.0165819316693031F, 0F, (-0.0331638633386063).toFloat(), 0F, 0.0165819316693031F
    )
    var bufferEMGFilter: ArrayList<FloatArray> = arrayListOf(
        floatArrayOf(0f, 0f, 0f, 0f, 0f)
    )
    /**肺音*/
    private val filterOrder2 = 2
    private val filterLen2 = filterOrder2 + 1
    /* a in Matlab */
    val denomCoeff4 = floatArrayOf(
        1F, 1.35637520883435F, 0.555164898708493F
    )
    /* b in Matlab */
    val numCoeff4 = floatArrayOf(
        0.743600731769730F,
        1.42433864400338F, 0.743600731769730F
    )
    /***********************************
     *  Parameter for Audio Filter.
     ***********************************/
    private val audioFilterOrder = 2
    private val audioFilterLen = audioFilterOrder * 2 + 1
    /* a in Matlab */
    val denomCoeff2 = floatArrayOf(
        1F,
        (-1.50435441824753).toFloat(), 0.282925668922224F,
        (0.0187548643473044).toFloat(), 0.204118003083023F
    )
    /* b in Matlab */
    val numCoeff2 = floatArrayOf(
        0.407988564001607F, 0F, (-0.815977128003214).toFloat(), 0F, 0.407988564001607F
    )

    var bufferAudioFilter: ArrayList<Float> = arrayListOf(0f, 0f, 0f, 0f, 0f)
    /***********************************
     *  Parameter for Up-sampling Filter.
     ***********************************/
    private val upSamplingFilterOrder = 2
    private val upSamplingFilterLen = upSamplingFilterOrder + 1
    /* a in Matlab */
    val denomCoeff3 = floatArrayOf(
        1F,
        (0).toFloat(), 0.333333333333333F,
        (0).toFloat()
    )
    /* b in Matlab */
    val numCoeff3 = floatArrayOf(
        0.166666666666667F, 0.5F, (0.5).toFloat(), 0.166666666666667F
    )

    var bufferUpSamplingFilter: ArrayList<Float> = arrayListOf(0f, 0f, 0f, 0f, 0f)

    fun signalFilter(value: Float, channel: Int) : Float {
        var result = 0f

        for (k in filterLen -1 downTo 1) {
            bufferEMGFilter[channel][k] = bufferEMGFilter[channel][k - 1]
        }

        bufferEMGFilter[channel][0] = value
        for (k in 1 until filterLen) {
            bufferEMGFilter[channel][0] -= denomCoeff[k] * bufferEMGFilter[channel][k]
        }

        for (k in 0 until filterLen) {
            result += numCoeff[k] * bufferEMGFilter[channel][k]
        }

        return result
    }
    fun signalFilter2(value: Float, channel: Int) : Float {
        var result = 0f

        for (k in filterLen2 -1 downTo 1) {
            bufferEMGFilter[channel][k] = bufferEMGFilter[channel][k - 1]
        }

        bufferEMGFilter[channel][0] = value
        for (k in 1 until filterLen2) {
            bufferEMGFilter[channel][0] -= denomCoeff4[k] * bufferEMGFilter[channel][k]
        }

        for (k in 0 until filterLen2) {
            result += numCoeff4[k] * bufferEMGFilter[channel][k]
        }

        return result
    }

    fun audioUpSampling(input : FloatArray) : ArrayList<Float> {
        val upSamplingArray: ArrayList<Float> = arrayListOf()

        for (ii in 0 until input.size-1) {
            upSamplingArray[ii*2] = input[ii]
            upSamplingArray[ii*2+1] = (input[ii] + input[ii+1])/2
        }

        upSamplingArray[(input.size-1)*2] = input[input.size-1]
        upSamplingArray[(input.size-1)*2+1] = input[input.size-1]

        return upSamplingArray
    }

    fun upSamplingFilter(value: Float): Float {
        var result = 0f

        for (k in upSamplingFilterLen -1 downTo 1) {
            bufferUpSamplingFilter[k] = bufferUpSamplingFilter[k - 1]
        }

        bufferUpSamplingFilter[0] = value
        for (k in 1 until upSamplingFilterLen) {
            bufferUpSamplingFilter[0] -= denomCoeff3[k] * bufferUpSamplingFilter[k]
        }

        for (k in 0 until upSamplingFilterLen) {
            result += numCoeff3[k] * bufferUpSamplingFilter[k]
        }

        return result
    }

    fun audioFilter(value: Float): Float {
        var result = 0f

        for (k in audioFilterLen -1 downTo 1) {
            bufferAudioFilter[k] = bufferAudioFilter[k - 1]
        }

        bufferAudioFilter[0] = value
        for (k in 1 until audioFilterLen) {
            bufferAudioFilter[0] -= denomCoeff2[k] * bufferAudioFilter[k]
        }

        for (k in 0 until audioFilterLen) {
            result += numCoeff2[k] * bufferAudioFilter[k]
        }

        return result
    }

}