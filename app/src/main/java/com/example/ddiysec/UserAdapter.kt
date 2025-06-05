package com.example.ddiysec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val userList: List<User>,
    private val listener: OnUserClickListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    interface OnUserClickListener {
        fun onUserClick(user: User)
        fun onUserLongClick(user: User, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user, listener)
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserId: TextView = itemView.findViewById(R.id.tv_user_id)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)

        fun bind(user: User, listener: OnUserClickListener) {
            tvUserId.text = "ID: ${user.id}"
            tvUserName.text = user.name
            tvUserEmail.text = user.email

            itemView.setOnClickListener {
                listener.onUserClick(user)
            }

            itemView.setOnLongClickListener {
                listener.onUserLongClick(user, adapterPosition)
                true
            }
        }
    }
}