package info.nightscout.androidaps.plugins.pump.ypsopump.dialog

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerAppCompatActivity
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.common.driver.PumpDriverConfigurationCapable
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryDataProvider
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryText
import info.nightscout.androidaps.plugins.pump.ypsopump.R
import info.nightscout.androidaps.plugins.pump.ypsopump.databinding.PumpHistoryActivityBinding
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import javax.inject.Inject

class PumpHistoryActivity : DaggerAppCompatActivity() {

    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var activePlugin: ActivePlugin

    var filteredHistoryList: MutableList<PumpHistoryEntry> = mutableListOf()
    var typeListFull: List<TypeList>? = null
    var fullList: MutableList<PumpHistoryEntry> = mutableListOf()

    private lateinit var historyDataProvider: PumpHistoryDataProvider
    private lateinit var binding: PumpHistoryActivityBinding

    var manualChange = false

    // TODO database inject
    //@Inject lateinit var  medtronicHistoryData: MedtronicHistoryData

    //@Inject lateinit var ypsoPumpDataConverter: YpsoPumpDataConverter
    //@Inject lateinit var ypsoPumpHistory: YpsoPumpHistory

    lateinit var historyTypeSpinner: Spinner
    lateinit var statusView: TextView
    lateinit var recyclerView: RecyclerView

    //lateinit var llm: LinearLayoutManager
    lateinit var recyclerViewAdapter: RecyclerViewAdapter

    private fun prepareData() {
        // val gc = GregorianCalendar()
        // gc.add(Calendar.HOUR_OF_DAY, -24)
        // fullList.addAll(erosHistory.getAllErosHistoryRecordsFromTimestamp(gc.timeInMillis))

        val allSince = historyDataProvider.getData() //.ypsoPumpHistory.getHistoryRecords()

        //ypsoPumpHistory.getHistoryRecordsAfter(DateTimeUtil.toATechDate(DateTime().minusDays(3)))
        this.fullList.clear()

        aapsLogger.info(LTag.PUMP, "Loaded ${allSince.size} items from database (age 3 days).")

        for (historyRecordEntity in allSince) {
            //val domainObject = ypsoPumpHistory.historyMapper.entityToDomain(historyRecordEntity)
            this.fullList.add(historyRecordEntity)
        }
    }

    private fun filterHistory(group: PumpHistoryEntryGroup) {
        filteredHistoryList.clear()

        if (group === PumpHistoryEntryGroup.All) {
            filteredHistoryList.addAll(fullList)
        } else {
            for (pumpHistoryEntry in fullList) {
                if (pumpHistoryEntry.getEntryTypeGroup() === group) {
                    filteredHistoryList.add(pumpHistoryEntry)
                }
            }
        }

        aapsLogger.info(LTag.PUMP, "Filtered list ${filteredHistoryList.size} items (group ${group}), from full list (${fullList.size}).")

        recyclerViewAdapter.setHistoryListInternal(filteredHistoryList)
        recyclerViewAdapter.notifyDataSetChanged()

        //LOG.debug("Items on filtered list: {}", filteredHistoryList.size());
    }

    override fun onResume() {
        super.onResume()

        filterHistory(selectedGroup)
        setHistoryTypeSpinner()
    }

    private fun setHistoryTypeSpinner() {
        manualChange = true
        for (i in typeListFull!!.indices) {
            if (typeListFull!![i].entryGroup === selectedGroup) {
                historyTypeSpinner.setSelection(i)
                break
            }
        }
        SystemClock.sleep(200)
        manualChange = false
    }

    // override fun onPause() {
    //     super.onPause()
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PumpHistoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuration
        val activePump = activePlugin.activePump

        if (activePump is PumpDriverConfigurationCapable) {
            val pumpHistoryDataProvider = activePump.getPumpDriverConfiguration().getPumpHistoryDataProvider()
        } else {
            throw RuntimeException("PumpBLEConfigActivity can be used only with PumpDriverConfigurationCapable pump driver.")
        }

        title = historyDataProvider.getText(PumpHistoryText.PUMP_HISTORY)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // setContentView(R.layout.ypsopump_history_activity)

        prepareData()

        historyTypeSpinner = findViewById(R.id.ypsopump_historytype)
        statusView = findViewById(R.id.ypsopump_historystatus)
        recyclerView = findViewById(R.id.ypsopump_history_recyclerview)
        recyclerView.setHasFixedSize(true)
        //llm = LinearLayoutManager(this)
        recyclerView.layoutManager = LinearLayoutManager(this)  //.setLayoutManager(llm)
        recyclerViewAdapter = RecyclerViewAdapter(filteredHistoryList)
        recyclerView.adapter = recyclerViewAdapter
        statusView.visibility = View.GONE
        typeListFull = getTypeList(PumpHistoryEntryGroup.getTranslatedList(resourceHelper))
        val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_centered, typeListFull!!)
        historyTypeSpinner.adapter = spinnerAdapter
        historyTypeSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (manualChange) return
                val selected = historyTypeSpinner.getSelectedItem() as TypeList
                showingType = selected
                selectedGroup = selected.entryGroup
                filterHistory(selectedGroup)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (manualChange) return
                filterHistory(PumpHistoryEntryGroup.All)
            }
        })
    }

    private fun getTypeList(list: List<PumpHistoryEntryGroup>?): List<TypeList> {
        val typeList = ArrayList<TypeList>()
        for (pumpHistoryEntryGroup in list!!) {
            typeList.add(TypeList(pumpHistoryEntryGroup))
        }
        return typeList
    }

    class TypeList internal constructor(var entryGroup: PumpHistoryEntryGroup) {

        var name: String
        override fun toString(): String {
            return name
        }

        init {
            name = entryGroup.translated!!
        }
    }

    class RecyclerViewAdapter internal constructor(
        var historyList: List<PumpHistoryEntry>
    ) : RecyclerView.Adapter<RecyclerViewAdapter.HistoryViewHolder>() {

        fun setHistoryListInternal(historyList: List<PumpHistoryEntry>) {
            // this.historyList.clear();
            // this.historyList.addAll(historyList);
            this.historyList = historyList

            // this.notifyDataSetChanged();
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): HistoryViewHolder {
            val v = LayoutInflater.from(viewGroup.context).inflate(
                R.layout.pump_history_item,  //
                viewGroup, false
            )
            return HistoryViewHolder(v)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val record = historyList[position]
            holder.timeView.text = record.getEntryDateTime()
            holder.typeView.text = record.getEntryType()
            holder.valueView.text = record.getEntryValue()
        }

        override fun getItemCount(): Int {
            return historyList.size
        }

        // override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        //     super.onAttachedToRecyclerView(recyclerView)
        // }

        class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            var timeView: TextView
            var typeView: TextView
            var valueView: TextView

            init {
                // cv = (CardView)itemView.findViewById(R.id.rileylink_history_item);
                timeView = itemView.findViewById(R.id.ypsopump_history_time)
                typeView = itemView.findViewById(R.id.ypsopump_history_source)
                valueView = itemView.findViewById(R.id.ypsopump_history_description)
            }
        }

    }

    companion object {

        var showingType: TypeList? = null
        var selectedGroup = PumpHistoryEntryGroup.All
    }
}