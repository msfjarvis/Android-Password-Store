/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.msfjarvis.aps.databinding.ExtraContentItemBinding

class ExtraContentAdapter(private val items: List<Pair<String, String>>) : RecyclerView.Adapter<ExtraContentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ExtraContentItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(private val binding: ExtraContentItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<String, String>) {
            binding.textview.hint = pair.first
            binding.textview.setText(pair.second)
        }
    }
}
