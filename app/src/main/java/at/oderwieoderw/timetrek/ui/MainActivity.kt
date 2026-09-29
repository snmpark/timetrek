package at.oderwieoderw.timetrek.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import at.oderwieoderw.timetrek.R
import at.oderwieoderw.timetrek.data.local.TimeEntryStore
import at.oderwieoderw.timetrek.domain.TimeEntry
import at.oderwieoderw.timetrek.ui.calendar.CalendarScreen
import at.oderwieoderw.timetrek.ui.tracking.TrackingScreen

class MainActivity : ComponentActivity() {
    private enum class Page { TRACK, CALENDAR }

    private lateinit var store: TimeEntryStore
    private lateinit var trackingScreen: TrackingScreen
    private lateinit var calendarScreen: CalendarScreen
    private lateinit var trackPage: View
    private lateinit var calendarPage: View
    private lateinit var trackControls: View
    private lateinit var trackTab: Button
    private lateinit var calendarTab: Button
    private lateinit var backToTrack: OnBackPressedCallback
    private var currentPage = Page.TRACK

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.main_root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        store = TimeEntryStore(this)
        trackingScreen = TrackingScreen(this, store, ::render)
        calendarScreen = CalendarScreen(
            this, store, savedInstanceState?.getLong("selected_date") ?: System.currentTimeMillis(), ::render
        )
        currentPage = if (savedInstanceState?.getString("page") == Page.CALENDAR.name) Page.CALENDAR else Page.TRACK

        trackPage = findViewById(R.id.track_page)
        calendarPage = findViewById(R.id.calendar_page)
        trackControls = findViewById(R.id.track_controls)
        trackTab = findViewById(R.id.track_tab)
        calendarTab = findViewById(R.id.calendar_tab)

        backToTrack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { showPage(Page.TRACK) }
        }
        onBackPressedDispatcher.addCallback(this, backToTrack)
        trackTab.setOnClickListener { showPage(Page.TRACK) }
        calendarTab.setOnClickListener { showPage(Page.CALENDAR) }
        showPage(currentPage)
        render()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        tick.run()
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("selected_date", calendarScreen.selectedDate)
        outState.putString("page", currentPage.name)
        super.onSaveInstanceState(outState)
    }

    private fun showPage(page: Page) {
        currentPage = page
        val isTracking = page == Page.TRACK
        trackPage.visibility = if (isTracking) View.VISIBLE else View.GONE
        trackControls.visibility = if (isTracking) View.VISIBLE else View.GONE
        calendarPage.visibility = if (isTracking) View.GONE else View.VISIBLE
        trackTab.isSelected = isTracking
        calendarTab.isSelected = !isTracking
        backToTrack.isEnabled = !isTracking
    }

    private fun render() {
        val now = System.currentTimeMillis()
        val active = store.activeStart
        val saved = store.entries()
        val entries = saved + listOfNotNull(active?.let { TimeEntry(it, maxOf(now, it + 1L)) })
        trackTab.setText(if (active == null) R.string.track_tab else R.string.track_live_tab)
        trackingScreen.render(now, entries, active)
        calendarScreen.render(now, saved, active)
    }
}
