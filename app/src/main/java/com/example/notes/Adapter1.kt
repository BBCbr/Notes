package com.example.notes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class Adapter1(
    var items: List<Noteitem>,
    var context: Context,
    private val listener: OnButtonClickListener
) : RecyclerView.Adapter<Adapter1.ViewHolder>() {

    interface OnButtonClickListener {
        fun onButtonClick(position: Int)
        fun onDeleteButtonClick(position: Int)
        fun onEditButtonClick(position: Int)
        fun onSelectButtonClick(position: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteShort: TextView = view.findViewById(R.id.note_short)
        val noteFull: TextView = view.findViewById(R.id.note_full)
        val delButton: Button = view.findViewById(R.id.but_del)
        val editButton: Button = view.findViewById(R.id.but_edit)
        val selectButton: Button = view.findViewById(R.id.but_select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.noteShort.text = item.note
        holder.noteFull.text = item.note

        holder.noteShort.setOnClickListener {
            holder.noteShort.visibility = View.GONE
            holder.noteFull.visibility = View.VISIBLE
        }

        holder.noteFull.setOnClickListener {
            holder.noteShort.visibility = View.VISIBLE
            holder.noteFull.visibility = View.GONE
        }

        holder.delButton.setOnClickListener {
            listener.onDeleteButtonClick(position)
        }

        holder.editButton.setOnClickListener {
            listener.onEditButtonClick(position)
        }


        holder.selectButton.setOnClickListener {
            listener.onSelectButtonClick(position)
        }
    }
}