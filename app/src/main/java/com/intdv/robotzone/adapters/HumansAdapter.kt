package com.intdv.robotzone.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aldebaran.qi.sdk.`object`.human.Human
import com.intdv.robotzone.databinding.ItemHumanBinding

class HumansAdapter(private val listener: IHumanListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val humans = mutableListOf<Human>()

    @SuppressLint("NotifyDataSetChanged")
    fun setHumans(items: List<Human>) {
        this.humans.clear()
        this.humans.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ItemHumanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HumansViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val human = humans[position]

        with(holder as HumansViewHolder) {
            bind(human, position)
        }
    }

    override fun getItemCount(): Int {
        return humans.size
    }

    interface IHumanListener {
        fun onHumanClicked(human: Human)
    }

    inner class HumansViewHolder(
        private val binding: ItemHumanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(human: Human, position: Int) {
            binding.apply {
                tvHuman.text = "Human $position"
                root.setOnClickListener {
                    listener.onHumanClicked(human)
                }
            }
        }
    }
}