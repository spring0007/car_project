package com.launcher.yfd_ui6.chemo2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui6.R

class LetterAdapter (
    private val letters: List<Char>,
    private val showCustomEntry: Boolean = true,
    private val onLetterClick: (Char) -> Unit
) : RecyclerView.Adapter<LetterAdapter.ViewHolder>() {
    
    // 过滤后的字母列表（如果不需要显示自定义入口，则排除 @ 符号）
    private val displayLetters = if (!showCustomEntry) {
        letters.filter { it != '@' }
    } else {
        letters
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLetter: TextView = view.findViewById(R.id.tv_letter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val letter = displayLetters[position]
        holder.itemView.visibility = View.VISIBLE
        holder.tvLetter.text = letter.toString()
        holder.tvLetter.visibility = View.VISIBLE
        holder.itemView.setOnClickListener { onLetterClick(letter) }
    }

    override fun getItemCount() = displayLetters.size
}