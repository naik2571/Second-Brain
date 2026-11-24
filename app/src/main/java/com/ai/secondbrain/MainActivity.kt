package com.ai.secondbrain

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.secondbrain.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: BrainDatabase
    private val notes = mutableListOf<NoteItem>()
    private lateinit var adapter: NoteAdapter
    private var minThreshold = 0.35f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = BrainDatabase(this)

        adapter = NoteAdapter(notes) { note ->
            AlertDialog.Builder(this)
                .setTitle(note.title)
                .setMessage(note.content + "\n\nTags: " + note.tags + "\nSemantic Score: " + (note.similarity * 100).toInt() + "%")
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete") { _, _ ->
                    db.deleteNote(note.id)
                    runSearch(binding.etSearch.text.toString())
                }
                .show()
        }

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { runSearch(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.sbThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                minThreshold = progress / 100.0f
                binding.tvThresholdLabel.text = "Min Match: ${progress}%"
                runSearch(binding.etSearch.text.toString())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnAddNote.setOnClickListener { showAddNoteDialog() }
        binding.btnSeedDemo.setOnClickListener { seedDemoKnowledgeBase() }

        runSearch("")
    }

    private fun runSearch(query: String) {
        notes.clear()
        if (query.isBlank()) {
            notes.addAll(db.getAllNotes())
            binding.tvResultCount.text = "${notes.size} total thoughts stored"
        } else {
            val results = db.semanticSearch(query, minThreshold)
            notes.addAll(results)
            binding.tvResultCount.text = "${results.size} semantic matches found"
        }
        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun seedDemoKnowledgeBase() {
        val samples = listOf(
            Triple("Customer Discovery Insights", "Talk to 20 users before writing any code. Focus on their pain points and daily habits instead of pitching your product idea.", "Startup, Business, Interviews"),
            Triple("Ketogenic Fuel & Avocado Diet", "High healthy fats with avocados, olive oil, and wild salmon maintain steady blood glucose levels and eliminate afternoon brain fog.", "Nutrition, Diet, Health, Workout"),
            Triple("Android Flow vs StateFlow", "StateFlow is a state-holder observable flow that emits the current and new state updates to its collectors. Replaces LiveData cleanly.", "Kotlin, Programming, Architecture, Tech"),
            Triple("The 4-Hour Deep Work Block", "Schedule uninterrupted 90-minute focus sessions in the morning without phone or Slack notifications. Energy beats time management.", "Productivity, Focus, Habits, Mindset"),
            Triple("Index Funds & Cash Flow", "Automate monthly SIP transfers into low-cost index funds. Reinvest dividends to compound interest over decades.", "Finance, Investment, Wealth, Money"),
            Triple("Local Vector Embeddings Architecture", "Computing embeddings on-device protects user privacy while enabling sub-millisecond semantic search without cloud APIs.", "AI, Machine Learning, Embeddings, Tech")
        )
        for (s in samples) {
            db.insertNote(s.first, s.second, s.third)
        }
        runSearch("")
        Toast.makeText(this, "Seeded 6 knowledge thoughts with vectors!", Toast.LENGTH_SHORT).show()
    }

    private fun showAddNoteDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }
        val etTitle = EditText(this).apply { hint = "Title (e.g. LLM Inference)" }
        val etContent = EditText(this).apply { hint = "Note Content / Thought" }
        val etTags = EditText(this).apply { hint = "Tags (e.g. AI, Tech)" }

        layout.addView(etTitle)
        layout.addView(etContent)
        layout.addView(etTags)

        AlertDialog.Builder(this)
            .setTitle("Capture Thought (Auto-Embedded)")
            .setView(layout)
            .setPositiveButton("Embed & Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                val tags = etTags.text.toString().trim()
                if (title.isNotBlank() && content.isNotBlank()) {
                    db.insertNote(title, content, tags)
                    runSearch(binding.etSearch.text.toString())
                    Toast.makeText(this, "Vector generated & stored!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class NoteAdapter(
    private val list: List<NoteItem>,
    private val onClick: (NoteItem) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvNoteTitle)
        val tvContent: TextView = v.findViewById(R.id.tvNoteContent)
        val tvTags: TextView = v.findViewById(R.id.tvNoteTags)
        val tvScore: TextView = v.findViewById(R.id.tvScoreBadge)
        val tvDate: TextView = v.findViewById(R.id.tvNoteDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvContent.text = item.content
        holder.tvTags.text = item.tags
        holder.tvDate.text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(item.timestamp))

        val pct = (item.similarity * 100).toInt()
        if (item.similarity > 0.0f) {
            holder.tvScore.visibility = View.VISIBLE
            holder.tvScore.text = "🎯 ${pct}% MATCH"
            when {
                pct >= 85 -> holder.tvScore.setTextColor(Color.parseColor("#10B981"))
                pct >= 65 -> holder.tvScore.setTextColor(Color.parseColor("#38BDF8"))
                else -> holder.tvScore.setTextColor(Color.parseColor("#F59E0B"))
            }
        } else {
            holder.tvScore.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = list.size
}
