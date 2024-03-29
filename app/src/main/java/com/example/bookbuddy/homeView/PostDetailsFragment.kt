package com.example.bookbuddy.homeView

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bookbuddy.databinding.FragmentPostDetailsBinding
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookbuddy.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar
import java.util.Locale

import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PostDetailsFragment : Fragment() {
    private var myname: String = ""
    private var myemail: String = ""
    private var mysurname: String = ""
    private var mydp: String = ""
    private var postId: String = ""
    private var uId: String = ""

    private var pPicture: ImageView? = null
    private var pName: TextView? = null
    private var pTime: TextView? = null
    private var more: ImageButton? = null
    private var pTitle: TextView? = null
    private var pDescription: TextView? = null
    private var pCommentCount: TextView? = null
    private var pLikeCount: TextView? = null
    private var typeComment: EditText? = null
    private var likebtn: ImageView? = null

    private var sendb: ImageButton? = null
    private var comPic: ImageView? = null
    private var profile: LinearLayout? = null
    private var recyclerViewCom: RecyclerView? = null
    private var progressBar: ProgressBar? = null

    private lateinit var commentList: MutableList<ModelComment>
    private lateinit var adapterComment: AdapterComment
    private lateinit var bindingPostDetails: FragmentPostDetailsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var db= Firebase.firestore
    private lateinit var databaseReference: DatabaseReference
    private var myuid: String = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingPostDetails = FragmentPostDetailsBinding.inflate(inflater, container, false)
        recyclerViewCom = bindingPostDetails.recycleComment

        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        postId = arguments?.getString("pid") ?: ""
        uId = arguments?.getString("uid") ?: ""

        commentList = ArrayList()
        adapterComment = AdapterComment(requireContext(), commentList, myuid, postId)
        recyclerViewCom?.adapter = adapterComment

        pPicture = bindingPostDetails.detailsPictureCo
        pName = bindingPostDetails.detailsUnameCo
        pTime = bindingPostDetails.detailsUtimeCo
        more = bindingPostDetails.detailsMorebtnCo
        pTitle = bindingPostDetails.detailsPtitleCo
        pDescription = bindingPostDetails.detailsDescriptCo
        pCommentCount = bindingPostDetails.detailsPcommentCount
        pLikeCount = bindingPostDetails.detailsPlikeCount
        likebtn = bindingPostDetails.detailsLikeIv

        comPic = bindingPostDetails.commentPic
        typeComment = bindingPostDetails.typeCommet
        sendb = bindingPostDetails.sendcomment

        profile = bindingPostDetails.profilelayoutCo
        progressBar = bindingPostDetails.detailsPB

        getCommentsCount()
        getLikesCount()
        checkIfLiked()
        getImage(uId, myuid,pPicture as CircleImageView, comPic as CircleImageView)

        more?.visibility = View.GONE
        if (uId == myuid) {
            more?.visibility = View.VISIBLE
        }

        return bindingPostDetails.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = bindingPostDetails.detailsPB
        recyclerViewCom?.adapter = adapterComment
        recyclerViewCom?.layoutManager = LinearLayoutManager(requireContext())

        currentUserInfo()
        loadComments()
        loadPostInfo()

        likebtn?.setOnClickListener {
            likePost() {
                getLikesCount()
                checkIfLiked()
            }
        }
        sendb?.setOnClickListener {
            postComment {
                typeComment?.setText("")
                getCommentsCount()
            }
        }
        more?.setOnClickListener {
            showMoreOptions()
        }

    }
    @SuppressLint("SuspiciousIndentation")
    private fun getImage(uid: String, myuid:String, imageView: CircleImageView, commentImageView: CircleImageView) {
        val userInfoRef = FirebaseDatabase.getInstance().getReference("userInfo").child(uid)
        userInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val imgUri = dataSnapshot.child("imgUrl").value.toString()
                    if (imgUri.isNotEmpty()) {
                        context?.let {
                            Glide.with(it)
                                .load(imgUri)
                                .into(imageView)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        val commInfoRef = FirebaseDatabase.getInstance().getReference("userInfo").child(myuid)
            commInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val imgUri = dataSnapshot.child("imgUrl").value.toString()
                    if (imgUri.isNotEmpty()) {
                        context?.let {
                            Glide.with(it)
                                .load(imgUri)
                                .into(commentImageView)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    private fun showMoreOptions() {
        val popupMenu = PopupMenu(context, more!!, Gravity.END)
        popupMenu.menu.add(android.view.Menu.NONE, 0, 0, "DELETE")
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId == 0) {
                val postsRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
                postsRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                        val homeFragment = HomeFragment()
                        setCurrentFragment(homeFragment)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to delete post: $e", Toast.LENGTH_SHORT).show()
                    }
            }
            false
        }
        popupMenu.show()
    }
    private fun currentUserInfo(){
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val userRef = databaseReference.child("userInfo").child(uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                         myname = snapshot.child("name").getValue(String::class.java).toString()
                         myemail = snapshot.child("mail").getValue(String::class.java).toString()
                        mysurname= snapshot.child("surname").getValue(String::class.java).toString()
                    } else {
                        Log.d("UserInfo", "User data not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserInfo", "Error reading user data: ${error.message}")
                }
            })
        }
    }
    private fun loadPostInfo() {

        val databaseReference = FirebaseDatabase.getInstance().getReference("Posts")
        val query: Query = databaseReference.orderByChild("ptime").equalTo(postId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataSnapshot1 in dataSnapshot.children) {
                    val post =
                        dataSnapshot1.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    if (post != null) {
                        val ptitle = post["title"] as? String ?: ""
                        val descriptions = post["description"] as? String ?: ""
                        val uimage = post["upic"] as? String ?: ""
                        val uemail = post["uemail"] as? String ?: ""
                        val hisname = post["uname"] as? String ?: ""
                        val ptime = post["ptime"] as? String ?: ""
                        val calendar = Calendar.getInstance(Locale.ENGLISH)
                        calendar.timeInMillis = ptime.toLong()
                        val timedate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString()
                        pName?.text = hisname
                        pTime?.text = timedate
                        pTitle?.text = ptitle
                        pDescription?.text = descriptions
                    }
                }
            }
                override fun onCancelled(databaseError: DatabaseError) {
                }

        })
    }
    private fun likePost(callback: () -> Unit) {
        val likesRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Likes").child(myuid)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    likesRef.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Unliked", Toast.LENGTH_SHORT).show()
                            callback.invoke()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to unlike: $e", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    likesRef.setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Liked", Toast.LENGTH_SHORT).show()
                            callback.invoke()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to like: $e", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun checkIfLiked() {
        val likesRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Likes").child(myuid)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    likebtn?.setImageResource(com.example.bookbuddy.R.drawable.heart_red)
                } else {
                    likebtn?.setImageResource(com.example.bookbuddy.R.drawable.heart)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun getLikesCount() {
        val likesRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Likes")
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val likesCount = dataSnapshot.childrenCount
                pLikeCount!!.text = "Likes: $likesCount"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                pLikeCount!!.text = "Error"
            }
        })
    }
    private fun getCommentsCount() {
        val commentsRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments")
        commentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val commentsCount = dataSnapshot.childrenCount
                pCommentCount!!.text = "Comments: $commentsCount"
            }
            override fun onCancelled(databaseError: DatabaseError) {
                pCommentCount!!.text = "Error"
            }
        })
    }
    private fun loadComments() {
        val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val comments: MutableList<ModelComment> = ArrayList()
                for (dataSnapshot1 in dataSnapshot.children) {
                    val modelComment: ModelComment? = dataSnapshot1.getValue(ModelComment::class.java)
                    modelComment?.let { comments.add(it) }
                }
                adapterComment.updateComments(comments)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }
    private fun postComment(callback: () -> Unit) {
        progressBar?.visibility = View.VISIBLE

        val commentss = typeComment?.text.toString().trim()
        if (commentss.isEmpty()) {

            Toast.makeText(requireContext(), "Empty comment", Toast.LENGTH_LONG).show()
            progressBar?.visibility = View.GONE
            return
        }
        val timestamp = System.currentTimeMillis().toString()
        val datarf = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments")
        val hashMap: MutableMap<String, Any> = HashMap()
        hashMap["ptime"] = timestamp
        hashMap["comment"] = commentss
        hashMap["uid"] = myuid
        hashMap["uemail"] = myemail
        hashMap["udp"] = mydp
        hashMap["uname"] = myname
        datarf.child(timestamp).setValue(hashMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Added", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_LONG).show()
            }
        progressBar?.visibility = View.GONE
        callback.invoke()
    }
    private fun setCurrentFragment(fragment: Fragment)=
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }

}