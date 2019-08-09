package com.example.mysns

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_write.*
import kotlinx.android.synthetic.main.card_background.view.*

class WriteActivity : AppCompatActivity() {

    var mode = "post"
    var postId =""
    var commentId=""
    var currentBgPosition = 0
    private lateinit var database: DatabaseReference


    val bgList = mutableListOf(
        "android.resource://com.example.mysns/drawable/default_bg"
        , "android.resource://com.example.mysns/drawable/bg2"
        , "android.resource://com.example.mysns/drawable/bg3"
        , "android.resource://com.example.mysns/drawable/bg4"
        , "android.resource://com.example.mysns/drawable/bg5"
        , "android.resource://com.example.mysns/drawable/bg6"
        , "android.resource://com.example.mysns/drawable/bg7"
        , "android.resource://com.example.mysns/drawable/bg8"
        , "android.resource://com.example.mysns/drawable/bg9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)
        (deleteButton as View).visibility =View.GONE


        intent.getStringExtra("mode")?.let{
            mode = intent.getStringExtra("mode")
            postId = intent.getStringExtra("postId")
        }

       // if(mode.equals("post")) "글쓰기" else if(mode.equals("comment"))"댓글쓰기"
        when{
            mode.equals("post") -> supportActionBar?.title ="글쓰기"
            mode.equals("comment") -> supportActionBar?.title ="댓글쓰기"
            mode.equals("commentEdit") -> {
                commentId = intent.getStringExtra("commentId")
                supportActionBar?.title = "댓글수정"
                (deleteButton as View).visibility =View.VISIBLE
                inputTextUpdate("Comments/$postId/$commentId/message")

            }
            else -> {
                supportActionBar?.title ="글수정"
                (deleteButton as View).visibility =View.VISIBLE
                inputTextUpdate("Posts/$postId/message")
            }
        }
        val layoutManager = LinearLayoutManager(this@WriteActivity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter()
        database = FirebaseDatabase.getInstance().reference


        sendButton.setOnClickListener {
            if(TextUtils.isEmpty(input.text)){
                Toast.makeText(applicationContext, "메세지를 입력하세요", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(mode.equals("post")){
                val post = Post()
                val newRef = FirebaseDatabase.getInstance().getReference("Posts").push()
                post.writeTime = ServerValue.TIMESTAMP
                post.bgUri = bgList[currentBgPosition]
                post.message = input.text.toString()
                post.writerId = getMyId()
                post.postId = newRef.key
                newRef.setValue(post)
                Toast.makeText(applicationContext, "공유되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }else if(mode.equals("comment")){
                val comment = Comment()
                commentCountUpdate("send")
                val newRef = FirebaseDatabase.getInstance().getReference("Comments/$postId").push()


                comment.writeTime = ServerValue.TIMESTAMP
                comment.bgUri = bgList[currentBgPosition]
                comment.message = input.text.toString()
                comment.writerId = getMyId()
                comment.commentId = newRef.key
                comment.postId = postId
                newRef.setValue(comment)


                Toast.makeText(applicationContext, "공유되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } else if(mode.equals("commentEdit")){
                val postEditUpdate = HashMap<String, Any>()
                postEditUpdate["/Comments/$postId/$commentId/message"] = input.text.toString()
                database.updateChildren(postEditUpdate)
                finish()
            }
            else{
                val postEditUpdate = HashMap<String, Any>()
                postEditUpdate["/Posts/$postId/message"] = input.text.toString()
                database.updateChildren(postEditUpdate)
                finish()
            }

        }

        deleteButton.setOnClickListener {
            if(mode.equals("commentEdit")){
                FirebaseDatabase.getInstance().getReference("Comments/$postId/$commentId").removeValue()
                commentCountUpdate("delete")
                finish()

            }else if(mode.equals("postEdit")){
                FirebaseDatabase.getInstance().getReference("Posts/$postId").removeValue()
                FirebaseDatabase.getInstance().getReference("Comments/$postId").removeValue()
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()

            }
        }

    }

    fun getMyId():String{
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    inner class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val imageView = itemView.imageView
    }

    inner class MyAdapter : RecyclerView.Adapter<MyViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(this@WriteActivity).inflate(R.layout.card_background, parent, false))
        }

        override fun getItemCount(): Int {
            return bgList.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

            Picasso.get().load(Uri.parse(bgList[position])).fit().centerCrop().into(holder.imageView)

            holder.itemView.setOnClickListener {
                currentBgPosition = position
                Picasso.get().load(Uri.parse(bgList[position])).fit().centerCrop().into(writeBackground)
            }
        }
    }
    fun commentCountUpdate(mode:String){
        var commentCount = 0
        val newRefPost =  FirebaseDatabase.getInstance().getReference("Posts/$postId/commentCount")


        newRefPost.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if(mode.equals("send")){
                    commentCount = snapshot.value.toString().toInt()+1
                }
                else{
                    commentCount = snapshot.value.toString().toInt()-1
                }
                val childUpdates = HashMap<String, Any>()
                childUpdates["/Posts/$postId/commentCount"] = commentCount.toString()
                database.updateChildren(childUpdates)
            }
        })
    }

    fun inputTextUpdate(referenceValue:String){
        val refEdit =  FirebaseDatabase.getInstance().getReference(referenceValue)

        refEdit.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                input.setText(snapshot.value.toString())
            }
        })
    }
}
