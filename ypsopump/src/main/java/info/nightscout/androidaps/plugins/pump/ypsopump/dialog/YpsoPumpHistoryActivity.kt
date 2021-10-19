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
import dagger.android.DaggerActivity
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.ypsopump.R
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.utils.resources.ResourceHelper
import java.util.*
import javax.inject.Inject

class YpsoPumpHistoryActivity : DaggerActivity() {

    // TODO database inject
    //@Inject lateinit var  medtronicHistoryData: MedtronicHistoryData
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var ypsoPumpDataConverter: YpsoPumpDataConverter

    lateinit var historyTypeSpinner: Spinner
    lateinit var statusView: TextView
    lateinit var recyclerView: RecyclerView
    //lateinit var llm: LinearLayoutManager
    lateinit var recyclerViewAdapter: RecyclerViewAdapter

    var filteredHistoryList: MutableList<EventDto> = ArrayList()
    var manualChange = false
    var typeListFull: List<TypeList>? = null


    private fun filterHistory(group: PumpHistoryEntryGroup) {
        filteredHistoryList.clear()
        val list: MutableList<EventDto> = ArrayList()
        // TODO read data from database
        //list.addAll(medtronicHistoryData.allHistory)

        //LOG.debug("Items on full list: {}", list.size());
        if (group === PumpHistoryEntryGroup.All) {
            filteredHistoryList.addAll(list)
        } else {
            for (pumpHistoryEntry in list) {
                if (pumpHistoryEntry.entryType.group === group) {
                    filteredHistoryList.add(pumpHistoryEntry)
                }
            }
        }

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
        setContentView(R.layout.ypsopump_history_activity)
        historyTypeSpinner = findViewById(R.id.ypsopump_historytype)
        statusView = findViewById(R.id.ypsopump_historystatus)
        recyclerView = findViewById(R.id.ypsopump_history_recyclerview)
        recyclerView.setHasFixedSize(true)
        //llm = LinearLayoutManager(this)
        recyclerView.layoutManager = LinearLayoutManager(this)  //.setLayoutManager(llm)
        recyclerViewAdapter = RecyclerViewAdapter(filteredHistoryList, resourceHelper, ypsoPumpDataConverter)
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

    class RecyclerViewAdapter internal constructor(var historyList: List<EventDto>, var resourceHelper: ResourceHelper, var ypsoPumpDataConverter: YpsoPumpDataConverter) : RecyclerView.Adapter<RecyclerViewAdapter.HistoryViewHolder>() {


        fun setHistoryListInternal(historyList: List<EventDto>) {
            // this.historyList.clear();
            // this.historyList.addAll(historyList);
            this.historyList = historyList

            // this.notifyDataSetChanged();
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): HistoryViewHolder {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.ypsopump_history_item,  //
                viewGroup, false)
            return HistoryViewHolder(v)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val record = historyList[position]
            //if (record != null) {
                holder.timeView.text = record.dateTimeString
                holder.typeView.text = record.entryType.name
                holder.valueView.text = record.getDisplayableValue(resourceHelper, ypsoPumpDataConverter)
            //}
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