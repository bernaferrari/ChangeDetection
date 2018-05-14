package com.example.changedetection.forms

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.example.changedetection.Consts
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.orhanobut.logger.Logger
import org.threeten.bp.LocalDate
import java.util.*

object FormSaveAndHook {

    internal fun areConditionsInvalid(
        fastFA: FastItemAdapter<EmptyAdapter>,
        ismember: Boolean
    ): Boolean {
        var map3isnull: Boolean? = null
        for (i in 0 until fastFA.itemCount) {
            val currentkind = fastFA.getAdapterItem(i).kind

            if (fastFA.getAdapterItem(i) is FormDecSection) {
                continue
            }

            when (currentkind) {
                FormConstants.iname -> {
                    if ((fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText() == null) {
                        return true
                    }
                }
                FormConstants.iaddress -> {
                    val tmp =
                        ((fastFA.getAdapterItem(i) as FormDecAddress).visibleHolder as FormDecAddress.ViewHolderAddress)
                    if (ismember) {
                        if (tmp.bairro.text == null || tmp.cidade.text == null || tmp.numero.text == null || tmp.rua.text == null) {
                            return true
                        }
                    } else {
                        if (tmp.bairro.text == null) {
                            return true
                        }
                    }
                }
                FormConstants.ibirthday -> {
                    if ((fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText() == null) {
                        return true
                    }
                }
                FormConstants.iphone -> {
                    if (map3isnull == null) {
                        map3isnull = (fastFA.getAdapterItem(i) as FormDecExpands).retrieveText() ==
                                null
                    }
                }
                else -> {
                }
            }
        }
        if (map3isnull == null || map3isnull == true) {
            return true
        }
        return false
    }

    internal fun saveData(
        fastFA: FastItemAdapter<EmptyAdapter>,
        nucleosIdList: MutableList<String>
    ): HashMap<String, Any> {

        val map2 = HashMap<String, Any>()
        var prevkind = 0
        val lst = mutableListOf<String>()

        for (i in 0 until fastFA.itemCount) {
            val currentkind = fastFA.getAdapterItem(i).kind

            //This need to happen on every single iteration, else some fields will not be added to the map
            if (lst.isNotEmpty() && (prevkind != currentkind || i == fastFA.itemCount - 1)) {
                FormConstants.serverNames[prevkind]?.let {
                    map2[it] = lst.toList()
                }
                lst.clear()
            }

            val kindfromservernames: String = FormConstants.serverNames[currentkind] ?: ""

            if (fastFA.getAdapterItem(i) is FormDecSection) {
                continue
            }

            when (currentkind) {
                FormConstants.iname, FormConstants.url -> {
                    (fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText()?.let {
                        map2[kindfromservernames] = it
                    }
                }
                FormConstants.ispouse, FormConstants.ibirthday, FormConstants.isinglephone, FormConstants.ipreletor, FormConstants.imeetingscheduledate -> {
                    (fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText()?.let {
                        map2[kindfromservernames] = it
                    }
                }
                FormConstants.iphone, FormConstants.iemail, FormConstants.isons -> {
                    (fastFA.getAdapterItem(i) as FormDecExpands).retrieveText()?.let {
                        lst.add(it)
                    }
                }
                FormConstants.iselectnucleo -> {
                    map2["participant"] = mutableMapOf<String, Any>().apply {
                        val retrieved =
                            (fastFA.getAdapterItem(i) as FormDecSingleText).retrieveSelected()
                        if (retrieved.isEmpty()) {
                            if ((fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText() == "Não, mas gostaria de participar.") {
                                map2["not participating"] = mutableMapOf<String, Any>().apply {
                                    this["would like"] = true
                                    this[Consts.keyTimestamp] = LocalDate.now().toEpochDay()
                                }
                            } else if ((fastFA.getAdapterItem(i) as FormDecSingleText).retrieveText() == "Não, e não gostaria de participar.") {
                                this["not participating"] = mutableMapOf<String, Any>().apply {
                                    this["would like"] = false
                                    this[Consts.keyTimestamp] = LocalDate.now().toEpochDay()
                                }
                            }
                        } else {
                            retrieved.forEach {
                                this[nucleosIdList[it]] = mutableMapOf<String, Any>().apply {
                                    this["kind"] = "pending"
                                    this[Consts.keyTimestamp] = LocalDate.now().toEpochDay()
                                }
                            }
                        }
                    }
                }
                FormConstants.imemberfromipc -> {
                    (fastFA.getAdapterItem(i) as FormDecIsMember).isYesSelected().let {
                        map2[kindfromservernames] = it
                    }
                }
                else -> {

                }
            }
            prevkind = currentkind
        }
        Logger.d("map2... $map2")
        return map2
    }


    internal fun eventhook(fastFA: FastItemAdapter<EmptyAdapter>): ClickEventHook<EmptyAdapter> {
        return object : ClickEventHook<EmptyAdapter>() {
            override fun onClick(
                v: View?,
                position: Int,
                fastAdapter: FastAdapter<EmptyAdapter>?,
                item: EmptyAdapter?
            ) {
                fastFA.remove(position)
            }

            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {

                if (viewHolder !is FormDecExpands.ViewHolderSimpleExpands) {
                    return null
                }

                val a = (viewHolder).nome
                val b = (viewHolder).delete
                b.visibility = View.GONE

                a.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        val adapterposition = fastFA.getHolderAdapterPosition(viewHolder)

                        val adpt = (fastFA.getAdapterItem(adapterposition) as? FormDecExpands)
                        if (adapterposition > 0 && adpt?.primarytext == p0.toString()) {
                            b.visibility = View.VISIBLE
                            if (fastFA.adapterItems.last {
                                    it.kind == fastFA.getAdapterItem(
                                        adapterposition
                                    ).kind
                                } == fastFA.getAdapterItem(adapterposition)) {
                                b.visibility = View.GONE
                            }
                            return
                        }

                        (fastFA.getAdapterItem(fastFA.getHolderAdapterPosition(viewHolder)) as? FormDecExpands)?.primarytext =
                                p0.toString()

                        if (fastFA.adapterItems.last {
                                it.kind == fastFA.getAdapterItem(
                                    adapterposition
                                ).kind
                            } == fastFA.getAdapterItem(adapterposition)) {
                            val pos = fastFA.getHolderAdapterPosition(viewHolder)

                            fastFA.add(
                                pos + 1,
                                FormDecExpands("", fastFA.getAdapterItem(pos).kind)
                            )

                            b.visibility = View.VISIBLE
                        }
                    }

                    override fun beforeTextChanged(
                        p0: CharSequence?,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) = Unit

                    override fun onTextChanged(
                        p0: CharSequence?,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) = Unit
                })

                return b
            }
        }

//            override fun onClick(
//                v: View,
//                position: Int,
//                fastAdapter: FastAdapter<FormCreateAdapter>,
//                item: FormCreateAdapter
//            ) {
//                fastFA.remove(position)
//            }
    }

}