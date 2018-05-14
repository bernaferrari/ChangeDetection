package com.example.changedetection.forms

import android.app.FragmentManager
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.changedetection.R
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.util.*

internal class FormDateAndBirthday(
    val nome: String,
    override val kind: Int,
    val additional: Any? = null,
    val isEditing: Boolean = false
) :
    EmptyAdapter() {
    var primarytext = ""
    var visibleHolder: RecyclerView.ViewHolder? = null

    override fun getType(): Int = kind

    override fun getViewHolder(v: View) = ViewHolderNonExpanding(v)

    override fun getLayoutRes(): Int = R.layout.eureka_single_edittext

    var selectedcache = arrayOf<Int>()

    override fun detachFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolderNonExpanding) {
            primarytext = holder.nome.text.toString()
        }
        super.detachFromWindow(holder)
    }

    var datecache = GregorianCalendar.getInstance()

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        val holder = holder as ViewHolderNonExpanding
        visibleHolder = holder
        extensions.setImage(holder.whatsappimage, kind)
        holder.delete.visibility = View.GONE

        when (kind) {
            FormConstants.ibirthday, FormConstants.imeetingscheduledate -> {
                holder.nome.apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    hint = if (kind == FormConstants.imeetingscheduledate) {
                        "selecione"
                    } else {
                        "data de nascimento"
                    }
                    isFocusable = false
                }

                var now = GregorianCalendar.getInstance()

                (additional as? Pair<FragmentManager, Calendar>)?.second?.let {
                    now = it

                    if (kind == FormConstants.imeetingscheduledate || isEditing) {
                        val date: String =
                            String.format(
                                "%02d",
                                now.get(Calendar.DAY_OF_MONTH)
                            ) + "/" + String.format(
                                "%02d",
                                now.get(Calendar.MONTH) + 1
                            ) + "/" + now.get(Calendar.YEAR)
                        holder.nome.setText(date, TextView.BufferType.EDITABLE)
                        datecache = now
                    }
                }

                val dpd = DatePickerDialog.newInstance(
                    { _, year, monthOfYear, dayOfMonth ->
                        val date: String = String.format("%02d", dayOfMonth) + "/" + String.format(
                            "%02d",
                            monthOfYear + 1
                        ) + "/" + year
                        holder.nome.setText(date, TextView.BufferType.EDITABLE)
//                        primarytext = date
                        datecache.set(year, monthOfYear, dayOfMonth)
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
                )

                if (kind == FormConstants.ibirthday) {
                    dpd.showYearPickerFirst(true)
                }
                dpd.vibrate(false)

                holder.nome.setOnClickListener {
                    (additional as? Pair<FragmentManager, Calendar>)?.first?.let {
                        dpd.show(it, "Datepickerdialog")
                    }
                }
            }
            else -> {
            }
        }
    }

    internal fun retrieveText(): String? {
        val txtfrombind = (visibleHolder as? ViewHolderNonExpanding)?.let {
            val finalstring = it.nome.text.toString()
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
                return@let finalstring
            } else {
                return@let null
            }
        }

        if (txtfrombind == null) {
            val finalstring = nome
                .replace(Regex("\\s+"), " ")
                .trim()

            if (finalstring != "" && finalstring != "null" && finalstring != " ") {
                return finalstring
            } else {
                if (isEditing && additional != null) {
                    (additional as? Pair<FragmentManager, Calendar>)?.second?.let {
                        val now = it
                        return String.format(
                            "%02d",
                            now.get(Calendar.DAY_OF_MONTH)
                        ) + "/" + String.format(
                            "%02d",
                            now.get(Calendar.MONTH) + 1
                        ) + "/" + now.get(Calendar.YEAR)
                    }
                }
            }
            return null
        }
        return txtfrombind
    }

    internal fun retrieveDate(): Calendar? {
        return datecache
    }

    internal fun retrieveSelected(): Array<Int> {
        return selectedcache
    }

    internal class ViewHolderNonExpanding constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val nome: AppCompatEditText = v.findViewById(R.id.nome)
        internal var whatsappimage: ImageView = v.findViewById(R.id.imageView)
        internal val delete: ImageView = v.findViewById(R.id.delete)
    }
}