package com.launcher.yfd_ui01.chemo2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R

class LetterAdapter (
    private val letters: List<Char>,
    private val onLetterClick: (Char) -> Unit
    ) : RecyclerView.Adapter<LetterAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLetter: TextView = view.findViewById(R.id.tv_letter)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_letter, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val letter = letters[position]
            holder.tvLetter.text = letter.toString()
            holder.itemView.setOnClickListener { onLetterClick(letter) }
        }

        override fun getItemCount() = letters.size
    }