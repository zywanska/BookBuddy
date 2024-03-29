package com.example.bookbuddy.authenticationPart

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Spinner
import com.example.bookbuddy.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.shashank.sony.fancytoastlib.FancyToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore

//import com.example.bookbuddy.connection





class SignupActivity : AppCompatActivity() {
    private lateinit var bindingSignup: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var db= Firebase.firestore

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingSignup = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(bindingSignup.root)
        firebaseAuth = FirebaseAuth.getInstance()
        bindingSignup.btnSignupSubmit.setOnClickListener {
            val name = bindingSignup.etUserName.text.toString()
            val surname = bindingSignup.etUserSurname.text.toString()
            val mail = bindingSignup.etUserMail.text.toString()
            val password = bindingSignup.etPasswordFirst.text.toString()
            val password2 = bindingSignup.etPasswordSecond.text.toString()
            val spinner1: Spinner = bindingSignup.spinnerCity
            val city: String = spinner1.selectedItem.toString()
            val spinner2: Spinner = bindingSignup.spinnerRole
            val role: String = spinner2.selectedItem.toString()

            if (name.isEmpty() || surname.isEmpty() || mail.isEmpty()
                || password.isEmpty() || city.isEmpty() || role.isEmpty())
            {
                FancyToast.makeText(
                    this@SignupActivity,
                    "Please insert all the data",
                    FancyToast.LENGTH_SHORT,
                    FancyToast.ERROR,
                    true
                ).show()
            } else if (!isEmailValid(mail)) {
                FancyToast.makeText(
                    this,
                    "Please enter a valid email address",
                    FancyToast.LENGTH_SHORT,
                    FancyToast.ERROR,
                    true
                ).show()
            }
            else if (password.length < 6) {
                FancyToast.makeText(
                    this,
                    "Your password must be at least 6 characters long",
                    FancyToast.LENGTH_SHORT,
                    FancyToast.ERROR,
                    true
                ).show()
            }
            else if (password != password2) {
                FancyToast.makeText(
                    this,
                    "Your passwords must match",
                    FancyToast.LENGTH_SHORT,
                    FancyToast.ERROR,
                    true
                ).show()
            }
            else {
                firebaseAuth.createUserWithEmailAndPassword(mail, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            firebaseAuth.currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = firebaseAuth.currentUser?.uid
                                    val userMap = hashMapOf(
                                        "userId" to userId,
                                        "name" to name,
                                        "surname" to surname,
                                        "mail" to mail,
                                        "city" to city,
                                        "role" to role,
                                    )
                                    if (userId != null) {
                                        val databaseReference = FirebaseDatabase.getInstance()
                                            .getReference("userInfo")
                                        databaseReference.child(userId).setValue(userMap)
                                    }
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                }

                                FancyToast.makeText(
                                    this,
                                    "Please click link on your email to verify your account",
                                    FancyToast.LENGTH_LONG,
                                    FancyToast.CONFUSING,
                                    false
                                ).show()
                            }
                        } else {
                            FancyToast.makeText(
                                this,
                                "Error while creating user, try again",
                                FancyToast.LENGTH_SHORT,
                                FancyToast.ERROR,
                                false
                            ).show()
                        }
                    }
            }
        }
        bindingSignup.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }
}





