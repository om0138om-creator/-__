package com.myapp.alarm

import android.app.*
import android.content.*
import android.media.RingtoneManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var timePickerHour: NumberPicker
    private lateinit var timePickerMinute: NumberPicker
    private lateinit var timePickerPeriod: NumberPicker
    private lateinit var alarmLabel: EditText
    private lateinit var addAlarmBtn: Button
    private lateinit var alarmsRecyclerView: RecyclerView
    private lateinit var currentTimeText: TextView
    private lateinit var currentDateText: TextView
    
    private val alarmsList = mutableListOf<AlarmItem>()
    private lateinit var adapter: AlarmsAdapter
    private lateinit var alarmManager: AlarmManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // تهيئة العناصر
        initViews()
        setupTimePickers()
        setupRecyclerView()
        loadAlarms()
        startClock()
        
        // زر إضافة منبه
        addAlarmBtn.setOnClickListener {
            addNewAlarm()
        }
    }
    
    private fun initViews() {
        timePickerHour = findViewById(R.id.timePickerHour)
        timePickerMinute = findViewById(R.id.timePickerMinute)
        timePickerPeriod = findViewById(R.id.timePickerPeriod)
        alarmLabel = findViewById(R.id.alarmLabel)
        addAlarmBtn = findViewById(R.id.addAlarmBtn)
        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView)
        currentTimeText = findViewById(R.id.currentTimeText)
        currentDateText = findViewById(R.id.currentDateText)
        
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    private fun setupTimePickers() {
        // الساعات 1-12
        timePickerHour.minValue = 1
        timePickerHour.maxValue = 12
        timePickerHour.value = 7
        
        // الدقائق 0-59
        timePickerMinute.minValue = 0
        timePickerMinute.maxValue = 59
        timePickerMinute.setFormatter { String.format("%02d", it) }
        
        // صباحاً / مساءً
        timePickerPeriod.minValue = 0
        timePickerPeriod.maxValue = 1
        timePickerPeriod.displayedValues = arrayOf("صباحاً", "مساءً")
    }
    
    private fun setupRecyclerView() {
        adapter = AlarmsAdapter(
            alarmsList,
            onToggle = { position -> toggleAlarm(position) },
            onDelete = { position -> deleteAlarm(position) }
        )
        alarmsRecyclerView.layoutManager = LinearLayoutManager(this)
        alarmsRecyclerView.adapter = adapter
    }
    
    private fun startClock() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateClock()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }
    
    private fun updateClock() {
        val calendar = Calendar.getInstance()
        var hour = calendar.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
        
        currentTimeText.text = String.format("%02d:%02d:%02d %s", hour, minute, second, amPm)
        
        // التاريخ
        val days = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val months = arrayOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        
        val dayName = days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        
        currentDateText.text = "$dayName، $day $month $year"
    }
    
    private fun addNewAlarm() {
        val hour = timePickerHour.value
        val minute = timePickerMinute.value
        val isPM = timePickerPeriod.value == 1
        val label = alarmLabel.text.toString().ifEmpty { "منبه" }
        
        val alarm = AlarmItem(
            id = System.currentTimeMillis(),
            hour = hour,
            minute = minute,
            isPM = isPM,
            label = label,
            isActive = true
        )
        
        alarmsList.add(0, alarm)
        adapter.notifyItemInserted(0)
        alarmsRecyclerView.scrollToPosition(0)
        
        scheduleAlarm(alarm)
        saveAlarms()
        
        alarmLabel.text.clear()
        Toast.makeText(this, "تم إضافة المنبه ✓", Toast.LENGTH_SHORT).show()
    }
    
    private fun scheduleAlarm(alarm: AlarmItem) {
        val calendar = Calendar.getInstance()
        var hour24 = alarm.hour
        
        if (alarm.isPM && hour24 != 12) hour24 += 12
        else if (!alarm.isPM && hour24 == 12) hour24 = 0
        
        calendar.set(Calendar.HOUR_OF_DAY, hour24)
        calendar.set(Calendar.MINUTE, alarm.minute)
        calendar.set(Calendar.SECOND, 0)
        
        // إذا كان الوقت قد مضى، اضبط لليوم التالي
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    
    private fun cancelAlarm(alarm: AlarmItem) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    private fun toggleAlarm(position: Int) {
        val alarm = alarmsList[position]
        alarm.isActive = !alarm.isActive
        
        if (alarm.isActive) {
            scheduleAlarm(alarm)
        } else {
            cancelAlarm(alarm)
        }
        
        adapter.notifyItemChanged(position)
        saveAlarms()
    }
    
    private fun deleteAlarm(position: Int) {
        val alarm = alarmsList[position]
        cancelAlarm(alarm)
        alarmsList.removeAt(position)
        adapter.notifyItemRemoved(position)
        saveAlarms()
        Toast.makeText(this, "تم حذف المنبه", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveAlarms() {
        val prefs = getSharedPreferences("alarms", MODE_PRIVATE)
        val json = alarmsList.joinToString(";") { 
            "${it.id},${it.hour},${it.minute},${it.isPM},${it.label},${it.isActive}"
        }
        prefs.edit().putString("alarms_data", json).apply()
    }
    
    private fun loadAlarms() {
        val prefs = getSharedPreferences("alarms", MODE_PRIVATE)
        val json = prefs.getString("alarms_data", "") ?: ""
        
        if (json.isNotEmpty()) {
            json.split(";").forEach { item ->
                val parts = item.split(",")
                if (parts.size >= 6) {
                    alarmsList.add(AlarmItem(
                        id = parts[0].toLong(),
                        hour = parts[1].toInt(),
                        minute = parts[2].toInt(),
                        isPM = parts[3].toBoolean(),
                        label = parts[4],
                        isActive = parts[5].toBoolean()
                    ))
                }
            }
            adapter.notifyDataSetChanged()
        }
    }
}

// ===== Data Class =====
data class AlarmItem(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val isPM: Boolean,
    val label: String,
    var isActive: Boolean
)

// ===== Adapter =====
class AlarmsAdapter(
    private val alarms: List<AlarmItem>,
    private val onToggle: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<AlarmsAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTimeText)
        val labelText: TextView = view.findViewById(R.id.alarmLabelText)
        val toggleSwitch: Switch = view.findViewById(R.id.alarmSwitch)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        val period = if (alarm.isPM) "م" else "ص"
        holder.timeText.text = String.format("%02d:%02d %s", alarm.hour, alarm.minute, period)
        holder.labelText.text = alarm.label
        holder.toggleSwitch.isChecked = alarm.isActive
        holder.itemView.alpha = if (alarm.isActive) 1f else 0.5f
        
        holder.toggleSwitch.setOnClickListener { onToggle(position) }
        holder.deleteBtn.setOnClickListener { onDelete(position) }
    }
    
    override fun getItemCount() = alarms.size
}

// ===== Alarm Receiver =====
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("ALARM_LABEL") ?: "المنبه"
        
        // تشغيل صوت المنبه
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, alarmUri)
        ringtone.play()
        
        // اهتزاز
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
        }
        
        // إشعار
        val channelId = "alarm_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "المنبهات", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = Notification.Builder(context, channelId)
            .setContentTitle("⏰ المنبه")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
}
