//package com.example.openattendance.forms
//
//import android.arch.lifecycle.MutableLiveData
//import android.support.v7.widget.*
//import android.text.InputType
//import android.view.View
//import android.widget.ImageView
//import android.widget.TextView
//import com.example.openattendance.R
//import com.afollestad.materialdialogs.MaterialDialog
//import com.example.openattendance.PrimitiveAbstractItem
//import com.example.openattendance.pesquisarHospedeiro
//import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
//import com.orhanobut.logger.Logger
//
//internal class FormDialogHost(
//    val nome: String,
//    val id: String,
//    override val kind: Int,
//    val viewModel: SharedEscato,
//    val changedall: MutableLiveData<MutableMap<String, *>>
//) :
//    EmptyAdapter() {
//    var visibleHolder: RecyclerView.ViewHolder? = null
//    var currentId = id
//    var primarytext = nome
//
//    override fun getType(): Int = kind
//
//    override fun getViewHolder(v: View) = ViewHolderNonExpanding(v)
//
//    override fun getLayoutRes(): Int = R.layout.eureka_single_edittext
//
//    var selectedcache = arrayOf<Int>()
//
//    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
//        super.bindView(holder, payloads)
//        val holder = holder as ViewHolderNonExpanding
//        visibleHolder = holder
//        extensions.setImage(holder.whatsappimage, kind)
//        holder.delete.visibility = View.GONE
//
//        when (kind) {
////            FormConstants.iname, FormConstants.url, FormConstants.ipreletor, FormConstants.isinglephone -> { // FULL NAME
////                holder.nome.apply {
////                    text = extensions.setTextFromCacheOrFunction(primarytext, nome)
////                    primarytext = this.text.toString()
////                    inputType = InputType.TYPE_CLASS_TEXT
////                    hint = FormConstants.hintNames[kind]
////                }
////            }
//            FormConstants.url -> {
//                holder.nome.apply {
//                    text = extensions.setTextFromCacheOrFunction(primarytext, nome)
//                    inputType = InputType.TYPE_CLASS_TEXT
//                    hint = "selecione o hospedeiro"
//                    isFocusable = false
//                }
//
//                holder.nome.setOnClickListener {
//
//                    val materialdialogpiece = MaterialDialog.Builder(holder.nome.context)
//                        .title("Selecionar hospedeiro")
//                        .customView(R.layout.dialog_customview_search, false)
//                        .negativeText("Cancelar")
//                        .positiveText("Confirmar")
//                        .onPositive { dialog, _ ->
//
//                            val text = dialog.customView?.findViewById<SearchView>(R.id.searchview)
//                                ?.query.toString()
//                            changedall.value = mutableMapOf<String, Any>().apply {
//                                this["id"] = ""
//                                this["name"] = text
//                            }
//                            currentId = ""
//                            primarytext = text
//                            holder.nome.setText(text, TextView.BufferType.EDITABLE)
//                        }
//
//                    val materialdialog = materialdialogpiece.build()
//
//                    val fastFA2 = FastItemAdapter<PrimitiveAbstractItem>()
//                    fastFA2.withOnClickListener { v, adapter, item, position ->
//                        changedall.value =
//                                (viewModel.repo.usersCache[item.stru.id]?.get("address") as? MutableMap<String, Any>
//                                        ?: mutableMapOf<String, Any>()).apply {
//                                    this["id"] = item.stru.id
//                                    this["name"] = item.stru.name
//                                }
//                        currentId = item.stru.id
//                        primarytext = item.stru.name
//                        holder.nome.setText(item.stru.name, TextView.BufferType.EDITABLE)
//                        materialdialog.dismiss()
//                        true
//                    }
//
//                    materialdialog.customView?.findViewById<ImageView>(R.id.savecontent)?.apply {
//                        this.visibility = View.GONE
//                    }
//
//                    val mSearchView =
//                        materialdialog.customView?.findViewById<SearchView>(R.id.searchview)
//                    mSearchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                        override fun onQueryTextSubmit(query: String?): Boolean {
//                            return true
//                        }
//
//                        override fun onQueryTextChange(newText: String?): Boolean {
//                            val searchText = newText ?: ""
//                            fastFA2.filter(searchText)
//                            fastFA2.itemFilter.withFilterPredicate { item, constraint ->
//                                if (item.stru is pesquisarHospedeiro) {
//                                    return@withFilterPredicate item.stru.name.toLowerCase().contains(
//                                        constraint.toString().toLowerCase()
//                                    ) ||
//                                            item.stru.address.toLowerCase().contains(constraint.toString().toLowerCase())
//                                }
//                                return@withFilterPredicate false
//                            }
//
//                            return true
//                        }
//                    })
//
//                    val recycler =
//                        materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)
//                    recycler?.apply {
//                        layoutManager = LinearLayoutManager(this.context)
//                        adapter = fastFA2
//                        itemAnimator = DefaultItemAnimator()
//                    }
//
//                    fastFA2.apply {
//                        this.clear()
//                        viewModel.repo.usersCache.forEach {
//                            Logger.d("pretty name: " + it.value["name"] as? String)
//
//                            val stringbuilder =
//                                objectHelper.addressToString(it.value["address"] as? MutableMap<*, *>)
//                            this.add(
//                                PrimitiveAbstractItem(
//                                    pesquisarHospedeiro(
//                                        it.value["name"] as? String ?: "",
//                                        it.key,
//                                        stringbuilder
//                                    )
//                                )
//                            )
//                        }
//                        sort(this)
//                    }
//
//                    materialdialog.show()
//                }
//            }
//            else -> {
//
//            }
//        }
//    }
//
//
//    private fun sort(fastFA: FastItemAdapter<PrimitiveAbstractItem>) {
//        // starred: true > false
//        fastFA.adapterItems.sortWith(compareBy<PrimitiveAbstractItem> { it.stru.name })
//    }
//
//    internal fun retrieveText(): String? {
//        val txtfrombind = (visibleHolder as? ViewHolderNonExpanding)?.let {
//            val finalstring = it.nome.text.toString()
//                .replace(Regex("\\s+"), " ")
//                .trim()
//
//            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
//                return@let finalstring
//            } else {
//                return@let null
//            }
//        }
//
//        if (txtfrombind == null) {
//            val finalstring = nome
//                .replace(Regex("\\s+"), " ")
//                .trim()
//            return null
//        }
//        return txtfrombind
//    }
//
//    internal fun retrieveSelected(): Array<Int> {
//        return selectedcache
//    }
//
//    internal class ViewHolderNonExpanding constructor(v: View) : RecyclerView.ViewHolder(v) {
//        internal val nome: AppCompatEditText = v.findViewById(R.id.nome)
//        internal var whatsappimage: ImageView = v.findViewById(R.id.imageView)
//        internal val delete: ImageView = v.findViewById(R.id.delete)
//    }
//}