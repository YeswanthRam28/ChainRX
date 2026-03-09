package com.example.chainrx

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chainrx.network.ApiClient
import com.example.chainrx.network.Proposal
import com.example.chainrx.network.ProposalCreate
import com.example.chainrx.network.ProposalVote
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class DAOActivity : AppCompatActivity() {

    private lateinit var rvProposals: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private var currentCommunity: Int = 0 // 0=General, 1=Hospital, 2=Transport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dao)

        rvProposals = findViewById(R.id.rvProposals)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tabLayout = findViewById(R.id.tabLayout)

        rvProposals.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentCommunity = tab?.position ?: 0
                loadProposals()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        swipeRefresh.setOnRefreshListener { loadProposals() }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.fabAddProposal).setOnClickListener { showCreateProposalDialog() }

        loadProposals()
    }

    private fun loadProposals() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val proposals = ApiClient.getService(this@DAOActivity).getProposals(currentCommunity)
                rvProposals.adapter = ProposalAdapter(proposals) { proposal, support ->
                    vote(proposal, support)
                }
            } catch (e: Exception) {
                Toast.makeText(this@DAOActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun vote(proposal: Proposal, support: Boolean) {
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@DAOActivity).voteProposal(ProposalVote(proposal.id, support))
                Toast.makeText(this@DAOActivity, "Vote submitted to blockchain!", Toast.LENGTH_SHORT).show()
                loadProposals()
            } catch (e: Exception) {
                Toast.makeText(this@DAOActivity, "Voting failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateProposalDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_proposal, null)
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val title = view.findViewById<EditText>(R.id.etTitle).text.toString()
                val desc = view.findViewById<EditText>(R.id.etDesc).text.toString()
                if (title.isNotBlank() && desc.isNotBlank()) {
                    createProposal(title, desc)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createProposal(title: String, desc: String) {
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@DAOActivity).createProposal(ProposalCreate(title, desc, currentCommunity))
                Toast.makeText(this@DAOActivity, "Proposal broadcasted!", Toast.LENGTH_SHORT).show()
                loadProposals()
            } catch (e: Exception) {
                Toast.makeText(this@DAOActivity, "Failed to create: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class ProposalAdapter(
        private val list: List<Proposal>,
        private val onVote: (Proposal, Boolean) -> Unit
    ) : RecyclerView.Adapter<ProposalAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.tvProposalTitle)
            val desc = v.findViewById<TextView>(R.id.tvProposalDesc)
            val vFor = v.findViewById<TextView>(R.id.tvVotesFor)
            val vAgainst = v.findViewById<TextView>(R.id.tvVotesAgainst)
            val expiry = v.findViewById<TextView>(R.id.tvExpiry)
            val actions = v.findViewById<View>(R.id.layoutActions)
            val votedStatus = v.findViewById<View>(R.id.tvVotedStatus)
            val btnFor = v.findViewById<Button>(R.id.btnVoteFor)
            val btnAgainst = v.findViewById<Button>(R.id.btnVoteAgainst)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_proposal, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = list[position]
            holder.title.text = p.title
            holder.desc.text = p.description
            holder.vFor.text = "✅ ${p.votesFor} For"
            holder.vAgainst.text = "❌ ${p.votesAgainst} Against"
            
            val timeLeft = p.endTime * 1000 - System.currentTimeMillis()
            holder.expiry.text = if (timeLeft > 0) {
                "Expires in " + DateUtils.getRelativeTimeSpanString(p.endTime * 1000)
            } else {
                "Governance Ended"
            }

            if (p.hasVoted || timeLeft <= 0) {
                holder.actions.visibility = View.GONE
                holder.votedStatus.visibility = View.VISIBLE
            } else {
                holder.actions.visibility = View.VISIBLE
                holder.votedStatus.visibility = View.GONE
                holder.btnFor.setOnClickListener { onVote(p, true) }
                holder.btnAgainst.setOnClickListener { onVote(p, false) }
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
