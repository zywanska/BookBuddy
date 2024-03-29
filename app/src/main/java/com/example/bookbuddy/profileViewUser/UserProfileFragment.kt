package com.example.bookbuddy.profileViewUser

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.ViewGroup
import com.example.bookbuddy.databinding.FragmentProfileBinding
import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Button
import com.bumptech.glide.Glide
import com.example.bookbuddy.R
import com.example.bookbuddy.authenticationPart.LoginActivity
import com.example.bookbuddy.profileViewAdmin.AdminFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


class UserProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var bindingProfile: FragmentProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var db = Firebase.firestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingProfile = FragmentProfileBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val userInfoRef =
                FirebaseDatabase.getInstance().getReference("userInfo").child(uid)

            userInfoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userRole = snapshot.child("role").getValue(String::class.java)
                    if (userRole == "Admin") {
                        bindingProfile.userProfileBackBtn.setOnClickListener {
                            setCurrentFragment(AdminFragment())
                        }
                    } else {
                        bindingProfile.userProfileBackBtn.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        userId?.let { fetchUserInfo(it) }

        return bindingProfile.root
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindingProfile.editProfileFab.setOnClickListener{
            val editProfileFragment = EditProfileFragment()
            setCurrentFragment(editProfileFragment)
        }
        bindingProfile.logoutButton.setOnClickListener{
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            FirebaseAuth.getInstance().signOut()
        }
        bindingProfile.favBooks.setOnClickListener{
            val favBookFragment = FavBookFragment()
            setCurrentFragment(favBookFragment)
        }
    }

    private fun fetchUserInfo(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("userInfo").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val name = dataSnapshot.child("name").getValue(String::class.java)
                    val surname = dataSnapshot.child("surname").getValue(String::class.java)
                    val email = dataSnapshot.child("mail").getValue(String::class.java)
                    val city = dataSnapshot.child("city").getValue(String::class.java)
                    val picture = dataSnapshot.child("imgUrl").getValue(String::class.java)
                    if (picture != null)
                    {
                        try {
                            context?.let { Glide.with(it).load(picture).into(bindingProfile.avatarIv) }
                        } catch (_: Exception) {
                        }
                    }
                    bindingProfile.textViewName.text = "Name: $name"
                    bindingProfile.textViewSurname.text = "Surname: $surname"
                    bindingProfile.textViewEmail.text = "Email: $email"
                    bindingProfile.textViewCity.text = "City: $city"

                    val logoutButton: Button = bindingProfile.logoutButton
                    logoutButton.setOnClickListener {
                        FirebaseAuth.getInstance().signOut()

                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun setCurrentFragment(fragment: Fragment)=
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }

}

