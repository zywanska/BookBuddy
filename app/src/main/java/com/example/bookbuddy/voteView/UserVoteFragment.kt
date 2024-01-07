package com.example.bookbuddy.voteView

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookbuddy.R
import com.example.bookbuddy.databinding.FragmentUserVotingBinding
import com.example.bookbuddy.databinding.FragmentVoteBinding
import com.example.bookbuddy.databinding.FragmentVotingBinding
import com.example.bookbuddy.databinding.UserVoteViewDesignBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserVoteFragment : Fragment() {
    private lateinit var bindingVote: FragmentUserVotingBinding
    private lateinit var bindingVoteDesigne: UserVoteViewDesignBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserVoteAdapter
    private var firebaseAuth = FirebaseAuth.getInstance()
    private  val bookIds = mutableListOf<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bindingVote = FragmentUserVotingBinding.inflate(inflater, container, false)
        bindingVoteDesigne = UserVoteViewDesignBinding.inflate(inflater, container, false)
        fetchAllBookIdsFromVoting()
        return bindingVote.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bookView = view.findViewById<LinearLayout>(R.id.Book)
        checkWinnerBranchExists()
    }

    private fun checkWinnerBranchExists() {
        val winnerReference = FirebaseDatabase.getInstance().getReference("Winner")

        winnerReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    fetchWinner()
                } else {
                    // Gałąź "Winner" nie istnieje, pobierz dane z głosowania
                    setupRecyclerView()
                    fetchVoteList()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error checking Winner branch: ${error.message}")
            }
        })
    }

    private fun fetchWinner() {
        val winnerReference = FirebaseDatabase.getInstance().getReference("Winner")

        winnerReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = mutableListOf<WinnerInfo>()

                for (childSnapshot in snapshot.children) {
                    val bookTitle = childSnapshot.child("bookTitle").getValue(String::class.java)
                    val numberOfVotes = childSnapshot.child("numberOfVotes").getValue(Int::class.java)
                    println(bookTitle)
                    bookTitle?.let { title ->
                        numberOfVotes?.let { votes ->
                            val winner = WinnerInfo(title, votes.toString())
                            data.add(winner)
                        }
                    }
                }

                // Wyświetlenie danych w logach
                data.forEach { winnerInfo ->
                    Log.d("WinnerInfo", "Book Title: ${winnerInfo.bookTitle}, Number of Votes: ${winnerInfo.totalVotes}")
                }

                // Aktualizacja interfejsu użytkownika (przykładowo):
                if (data.isNotEmpty()) {
                    val titleView = bindingVoteDesigne.titleView
                    val idIVBook = bindingVoteDesigne.idIVBook
                    val resultTextView = bindingVoteDesigne.Result
                    titleView.text = data[0].bookTitle
                    titleView.append("\nTotal Votes: ${data[0].totalVotes}")
                    println(titleView)


                    // Wyświetlenie liczby głosów w TextView o ID "Result"
                    resultTextView.text = "Ilosc głosow: ${data[0].totalVotes}"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching Winner data: ${error.message}")
            }
        })
    }
    private fun setupRecyclerView() {
        recyclerView = bindingVote.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val templist: List<VotingViewModel> = emptyList()
        val data: MutableList<VotingViewModel> = templist.toMutableList()
        adapter = UserVoteAdapter(data,
            onItemClick = { selectedItem -> /* Obsługa kliknięcia elementu */ },
            onNegativeVoteClick = { selectedItem -> /* Obsługa kliknięcia negatywnego głosu */ },
            onPositiveVoteClick = { selectedItem -> /* Obsługa kliknięcia pozytywnego głosu */ }
        )
        recyclerView.adapter = adapter
    }

    private fun fetchAllBookIdsFromVoting() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Voting")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children) {
                    val bookId = childSnapshot.key
                    // bookId to nazwa (klucz) dziecka w węźle "Voting"
                    Log.d("ChildKey", bookId ?: "Key is null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching children: ${error.message}")
            }
        })
    }
    private fun fetchVoteList() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val databaseReference = FirebaseDatabase.getInstance().getReference("Voting")
            val userVotesReference = FirebaseDatabase.getInstance().getReference("Voting")

            userVotesReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userVotesSnapshot: DataSnapshot) {
                    val data = mutableListOf<VotingViewModel>()

                    // Pobranie wszystkich książek dostępnych do głosowania
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (childSnapshot in snapshot.children) {
                                val bookId = childSnapshot.key ?: ""

                                // Sprawdzenie czy użytkownik zagłosował na tę konkretną książkę
                                val userVotedSnapshot = userVotesSnapshot.child(bookId)
                                val userVoted = userVotedSnapshot.child("usersVoted").child(uid).getValue(String::class.java)
                                println(userVoted)
                                if (userVoted!="true") {
                                    val title = childSnapshot.child("title").getValue(String::class.java)
                                    val publisher = childSnapshot.child("publisher").getValue(String::class.java)
                                    val thumbnail=childSnapshot.child("thumbnail").getValue(String::class.java).toString()
                                    title?.let { safeTitle ->
                                        publisher?.let { safePublisher ->
                                            data.add(VotingViewModel(safeTitle, safePublisher, 0, 0, bookId,thumbnail))
                                        }
                                    }
                                }
                            }
                            adapter.updateList(data)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w(ContentValues.TAG, "Error getting documents: ", databaseError.toException())
                        }
                    })
                }

                override fun onCancelled(userVotesError: DatabaseError) {
                    Log.e(ContentValues.TAG, "Error getting user votes: ", userVotesError.toException())
                }
            })
        }
    }


}