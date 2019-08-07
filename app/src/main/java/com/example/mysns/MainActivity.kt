package com.example.mysns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.recyclerView
import kotlinx.android.synthetic.main.activity_write.*
import kotlinx.android.synthetic.main.card_background.view.*
import kotlinx.android.synthetic.main.card_background.view.imageView
import kotlinx.android.synthetic.main.card_post.view.*
import kotlinx.android.synthetic.main.card_post.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.io.LineNumberReader
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val posts:MutableList<Post> = mutableListOf()
    val TAG = "MainActivity"

    val ref = FirebaseDatabase.getInstance().getReference("test")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        floatingActionButton.setOnClickListener {
            val intent = Intent(this@MainActivity, WriteActivity::class.java)
            startActivity(intent)
        }


     /*   ref.addValueEventListener(object:ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val message = snapshot.value.toString()

                Log.d(TAG, message)

                supportActionBar?.title = message
            }
        })*/

        val layoutMangager = LinearLayoutManager(this@MainActivity)

        layoutMangager.reverseLayout = true
        layoutMangager.stackFromEnd = true
        recyclerView.layoutManager = layoutMangager
        recyclerView.adapter = MyAdapter()

        FirebaseDatabase.getInstance().getReference("/Posts")
            .orderByChild("writeTime").addChildEventListener(object : ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, prevChildKey: String?) {
                    snapshot?.let{
                        snapshot -> val post = snapshot.getValue(Post::class.java)
                        post?.let{
                            if(prevChildKey == null){
                                posts.add(it)
                                recyclerView.adapter?.notifyItemInserted(posts.size -1)
                            }else{
                                val prevIndex = posts.map { it.postId }.indexOf(prevChildKey)
                                posts.add(prevIndex +1, post)
                                recyclerView.adapter?.notifyItemInserted(prevIndex + 1)
                            }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, prevChildKey: String?) {
                    snapshot.let{
                        snapshot -> val post = snapshot.getValue(Post::class.java)
                        post?.let{
                            val prevIndex = posts.map { it.postId }.indexOf(prevChildKey)
                            posts[prevIndex+1] = post
                            recyclerView.adapter?.notifyItemChanged(prevIndex+1)
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, prevChildKey: String?) {
                    snapshot?.let {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            val existIndex = posts.map { it.postId }.indexOf(post.postId)
                            posts.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemRemoved(existIndex)
                            if(prevChildKey == null){
                                posts.add(post)
                                recyclerView.adapter?.notifyItemChanged(posts.size -1)
                            }else{
                                val prevIndex = posts.map { it.postId}.indexOf(prevChildKey)
                                posts.add(prevIndex + 1, post)
                                recyclerView.adapter?.notifyItemChanged(prevIndex + 1)
                            }
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    snapshot?.let{
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            val existIndex = posts.map { it.postId }.indexOf(post.postId)
                            posts.removeAt(existIndex)
                            recyclerView.adapter?.notifyItemRemoved(existIndex)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    databaseError?.toException()?.printStackTrace()
                }
            })
    }

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imageView : ImageView = itemView.imageView
        val contentsText: TextView = itemView.contentsText
        val timeTextView:TextView = itemView.timeTextView
        val commentCountText:TextView = itemView.commentCountText
    }
    inner class MyAdapter : RecyclerView.Adapter<MyViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.card_post, parent, false))
        }

        override fun getItemCount(): Int {
            return posts.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = posts[position]
            Picasso.get().load(Uri.parse(post.bgUri)).fit().centerCrop().into(holder.imageView)
            holder.contentsText.text = post.message
            holder.timeTextView.text = getDiffTimeText(post.writeTime as Long)
            holder.commentCountText.text = "0"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra("postId", post.postId)

                startActivity(intent)
            }
        }

    }

    fun getDiffTimeText(targetTime:Long):String{
        val curDateTime = DateTime()
        val targetDateTime = DateTime().withMillis(targetTime)
        val diffDay = Days.daysBetween(curDateTime, targetDateTime).days
        val diffHours = Hours.hoursBetween(targetDateTime, curDateTime).hours
        val diffMinutes = Minutes.minutesBetween(targetDateTime, curDateTime).minutes

        if(diffDay == 0){
            if(diffHours == 0 && diffMinutes == 0){
                return "방금전"
            }
            return if(diffHours > 0){
                ""+ diffHours + "시간 전"
            }else "" + diffMinutes + "분전"
        }else{
            val format = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            return format.format(Date(targetTime))
        }

    }
}
