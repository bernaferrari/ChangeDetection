package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.robinhood.spark.SparkAdapter
import com.robinhood.spark.SparkView
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_spark.*

class DialogItemSpark(
    private val list: List<Int>,
    private val wasSuccessful: Boolean
) : Item() {

    override fun getLayout(): Int {
        return R.layout.item_spark
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val sparkView = viewHolder.sparkview
        sparkView.fillType = SparkView.FillType.DOWN
        sparkView.adapter = MyAdapter(list)
        sparkView.lineColor = if (wasSuccessful) {
            0xff356bf8.toInt()
        } else {
            0xfff04a43.toInt()
        }
    }

    inner class MyAdapter(private val yData: List<Int>) : SparkAdapter() {

        override fun getCount(): Int {
            return yData.size
        }

        override fun getItem(index: Int): Any {
            return yData[index]
        }

        override fun getY(index: Int): Float {
            return yData[index].toFloat()
        }
    }
}

