package com.example.bookbuddy.profileViewAdmin

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.bookbuddy.R
import com.example.bookbuddy.profileViewUser.UserProfileFragment
import com.example.bookbuddy.databinding.FragmentAdminBinding
import com.example.bookbuddy.searchView.SearchFragment
import com.example.bookbuddy.voteView.VoteFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale


class AdminFragment : Fragment(R.layout.fragment_admin) {
    private lateinit var bindingAdmin: FragmentAdminBinding

    private var firebaseAuth = FirebaseAuth.getInstance()
    var myuid: String? = firebaseAuth.currentUser?.uid
    var myuCity: String? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingAdmin = FragmentAdminBinding.inflate(inflater, container, false)

        return bindingAdmin.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("onViewCreated")
        bindingAdmin.idImgBtnAddUser.setOnClickListener {
            val userListFragment = UserListFragment()
            setCurrentFragment(userListFragment)
        }
        bindingAdmin.idImgBtnProfile.setOnClickListener {
            val userProfileFragment = UserProfileFragment()
            setCurrentFragment(userProfileFragment)
        }
        bindingAdmin.idImgBtnAddVote.setOnClickListener {
            val votingFragment = VoteFragment()
            setCurrentFragment(votingFragment)
        }
        bindingAdmin.idImgBtnAddDate.setOnClickListener {
            pickDateAndTime()
        }
    }
    private fun pickDateAndTime() {
        val cityRef = FirebaseDatabase.getInstance().getReference("userInfo").child(myuid!!)
        cityRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myuCity = dataSnapshot.child("city").getValue(String::class.java)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                activity?.let {
                    Toast.makeText(it, databaseError.message, Toast.LENGTH_LONG).show()
                }
            }
        })

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Date and Time")
        val scrollView = ScrollView(requireContext())
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(10, 10, 10, 10)

        val calendarView = CalendarView(requireContext())
        layout.addView(calendarView)

        val timePicker = TimePicker(requireContext())
        timePicker.setIs24HourView(true)
        layout.addView(timePicker)

        val saveButton = Button(requireContext())
        saveButton.text = "Save Date & Time"
        saveButton.setTextColor(ContextCompat.getColor(requireContext(), com.example.bookbuddy.R.color.activityBackground))
        layout.addView(saveButton)

        scrollView.addView(layout)
        builder.setView(scrollView)

        var meetingDate: Long = 0
        var meetingTime: String = ""

        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            meetingDate = calendar.timeInMillis
        }
        timePicker.setOnTimeChangedListener { view, hourOfDay, minute ->
            val timeFormat = if (hourOfDay >= 12) "PM" else "AM"
            meetingTime = String.format(
                "%02d:%02d %s",
                if (hourOfDay % 12 == 0) 12 else hourOfDay % 12,
                minute,
                timeFormat
            )
        }

        saveButton.setOnClickListener {
            if (meetingDate != 0L && meetingTime.isNotEmpty() && myuCity != null) {
                val databaseReference = FirebaseDatabase.getInstance().reference
                val meetingData = hashMapOf<String, Any>(
                    "date" to formatDate(meetingDate),
                    "time" to meetingTime,
                    "city" to myuCity!!
                )
                val updateData = HashMap<String, Any>()
                updateData["Meeting/${myuCity!!}"] = meetingData

                databaseReference.updateChildren(updateData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Meeting scheduled: ${formatDate(meetingDate)} $meetingTime in $myuCity",
                            Toast.LENGTH_SHORT
                        ).show()

                        alertDialog?.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to schedule meeting: $e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select both date and time",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        alertDialog = builder.create()
        alertDialog?.show()
    }
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun setCurrentFragment(fragment: Fragment)=
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }
}
