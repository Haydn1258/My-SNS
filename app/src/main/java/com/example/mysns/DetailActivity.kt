package com.example.mysns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    val commentList = mutableListOf<Comment>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

       (floatingActionButton3 as View).visibility =View.GONE
        //floatingActionButton3.hide()



        val postId = intent.getStringExtra("postId")
        val writeId = intent.getStringExtra("writeId")
        if(getMyId().equals(writeId)){
            (floatingActionButton3 as View).visibility =View.VISIBLE
        }


        val layoutManager = LinearLayoutManager(this@DetailActivity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdater()


        FirebaseDatabase.getInstance().getReference("/Posts/$postId")
            .addValueEventListener(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot?.let {
                        val post = it.getValue(Post::class.java)
                        post?.let {
                            Picasso.get().load(it.bgUri).into(backgroundImage)
                            contentsText.text = post.message
                        }
                    }
                }

            })

        FirebaseDatabase.getInstance().getReference("/Comments/$postId").addChildEventListener(object
            :ChildEventListener{
            override fun onCancelled(error: DatabaseError) {
                error?.toException()?.printStackTrace()
            }

            override fun onChildMoved(snapshot: DataSnapshot, prevChildKey: String?) {
                if(snapshot != null){
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let{
                        val existIndex = commentList.map { it.commentId }.indexOf(it.commentId)
                        commentList.removeAt(existIndex)

                        val prevIndex = commentList.map { it.commentId }.indexOf(prevChildKey)
                        commentList.add(prevIndex+1, it)
                        recyclerView.adapter?.notifyItemInserted(prevIndex+1)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, prevChildKey: String?) {
                snapshot?.let {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let{ comment ->
                        val prevIndex = commentList.map { it.commentId }.indexOf(prevChildKey)
                        commentList[prevIndex + 1] = comment
                        recyclerView.adapter?.notifyItemChanged(prevIndex + 1)

                    }
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, prevChildKey: String?) {
                snapshot?.let {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let{
                        val prevIndex = commentList.map { it.commentId }.indexOf(prevChildKey)
                        commentList.add(prevIndex+1, comment)
                        recyclerView.adapter?.notifyItemInserted(prevIndex + 1)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot?.let {
                    val comment= snapshot.getValue(Comment::class.java)
                    comment?.let{ comment ->
                        val existIndex = commentList.map { it.commentId }.indexOf(comment.commentId)
                        commentList.removeAt(existIndex)
                        recyclerView.adapter?.notifyItemRemoved(existIndex)
                    }
                }
            }
        })

        floatingActionButton2.setOnClickListener {
            val intent = Intent(this@DetailActivity, WriteActivity::class.java)
            intent.putExtra("mode","comment")
            intent.putExtra("postId", postId)
            startActivity(intent)
        }

        floatingActionButton3.setOnClickListener {
            val intent = Intent(this@DetailActivity, WriteActivity::class.java)
            intent.putExtra("mode","postEdit")
            intent.putExtra("postId", postId)
            startActivity(intent)
        }
    }

    inner class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val imageView = itemView.findViewById<ImageView>(R.id.background)
        val commentText = itemView.findViewById<TextView>(R.id.commentText)
    }

    inner class MyAdater : RecyclerView.Adapter<MyViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(this@DetailActivity).inflate(R.layout.card_comment, parent, false))
        }

        override fun getItemCount(): Int {
            return commentList.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val comment = commentList[position]
            comment?.let{
                Picasso.get().load(Uri.parse(comment.bgUri)).fit().centerCrop().into(holder.imageView)
                holder.commentText.text = comment.message
            }
            holder.imageView.setOnClickListener {
                if(getMyId().equals(comment.writerId)){
                    val intent = Intent(this@DetailActivity, WriteActivity::class.java)
                    intent.putExtra("mode","commentEdit")
                    intent.putExtra("postId", comment.postId)
                    intent.putExtra("writeId", comment.writerId)
                    intent.putExtra("commentId",comment.commentId)
                    startActivity(intent)
                }
            }
        }
    }
    fun getMyId():String{
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }
}
