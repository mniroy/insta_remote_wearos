package com.arashivision.sdk.demo.ui.wifi.adapter

import android.view.LayoutInflater
import android.view.View
import com.arashivision.sdk.demo.databinding.ItemSettingValueBinding
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter

class WiFiChannelAdapter(private val dataList: List<String>) : TagAdapter<String>(dataList) {

    override fun getView(parent: FlowLayout, position: Int, data: String): View {
        val bind = ItemSettingValueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        bind.tvSettingValue.text = data
        return bind.root
    }

    override fun onSelected(position: Int, view: View) {
        super.onSelected(position, view)
        view.isSelected = true
    }

    override fun unSelected(position: Int, view: View) {
        super.unSelected(position, view)
        view.isSelected = false
    }
}