package com.example.changedetection

import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon

class AdapterCarrosel(private val data: List<String>) :
    RecyclerView.Adapter<AdapterCarrosel.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_carrosel, parent, false)

        val r = v.context.resources
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4.toFloat(),
            r.displayMetrics
        ).toInt()

        val params =
            FrameLayout.LayoutParams(v.layoutParams.width, (v.layoutParams.width / 1.8).toInt())
        params.setMargins(px, px, px, px)
        v.layoutParams = params

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagem: String = data[position]
        val uri = Uri.parse(imagem)
        holder.fbsimpledrawee.setImageURI(uri, holder.fbsimpledrawee.context)
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fbsimpledrawee: SimpleDraweeView = itemView.findViewById(R.id.fbsimpledrawee)
    }
}

// -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //
class NestedRecycler(var n: Int)

class SeparadorNovo(var titulo: String)

open class NucleosTitulo(var titulo: String, var day: Int) {
    open var prio = 1
}

class NucleosInfo(
    titulo: String,
    day: Int,
    var hora: Int,
    var coordenadores: MutableMap<String, Any>,
    var id: String,
    var extra: Boolean = false
) : NucleosTitulo(titulo, day) {
    override var prio = 2
}

class VeryGenericAdapter(val stru: Any) :
    AbstractItem<VeryGenericAdapter, VeryGenericAdapter.ViewHolder>() {
    val fastAdapter: FastItemAdapter<VeryGenericAdapter> = FastItemAdapter()

    override fun getType(): Int {
        return when (stru) {
            is Principal_Item -> 0
            is Separador -> 1
            is SeparadorBig -> 2
            is DataProgramacao -> {
                return if (stru.tipo == "") 3
                else 4
            }
            is Ebd_item -> 5
            is String -> when (stru) {
                "Loading" -> 6
                else -> 7
            }
            is escalas_item -> 8
            is NestedRecycler -> 9
            is SeparadorNovo -> 10
            is NucleosInfo -> 11
            is NucleosTitulo -> 12
            is oneStringDiff_copy -> 13
            is oneStringDiff_copy2 -> 14
            else -> 14
        }
    }

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)
    override fun getLayoutRes(): Int {
        return when (stru) {
            is NestedRecycler -> R.layout.nested
            is Principal_Item -> R.layout.material_button_with_icon
            is Separador -> R.layout.fa_separador
            is SeparadorBig -> R.layout.big_appstore_today_title
            is DataProgramacao -> R.layout.nuc_nuc_item
            is Nucleos_item -> R.layout.nuc_nuc_item
            is String -> when (stru) {
                "Loading" -> R.layout.generic_loading
                else -> R.layout.item_sticky_generic
            }
            is Ebd_item -> R.layout.ebd_item
            is escalas_item -> R.layout.escalas_item
            is SeparadorNovo -> R.layout.main_separador_programacao
            is NucleosInfo -> R.layout.nuc_nuc_item
            is NucleosTitulo -> R.layout.big_appstore_today_title
            is oneStringDiff_copy -> R.layout.big_appstore_today_title
            is oneStringDiff_copy2 -> R.layout.item_nucleo_more
            else -> R.layout.fa_separador
        }
    }

    fun setDraw(btn: AppCompatButton?, icon: IIcon) {
        val cor = 0xff158c50.toInt()
        btn?.setCompoundDrawables(
            IconicsDrawable(
                btn.context,
                icon
            ).color(cor).sizeDp(36).paddingDp(6), null, null, null
        )
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        when (stru) {
            is Principal_Item -> {

                holder.btn?.text = stru.titulo
                holder.title?.text = stru.titulo
                val cor = 0xff03A9F4.toInt()
                //holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context,R.color.windowBackgroundNucleosTop))
//                val img:Drawable = IconicsDrawable(holder.btn!!.context, GoogleMaterial.Icon.gmd_3d_rotation)
//                holder.btn.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null)

                when (stru.icone) {
                    1 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_school)
                    2 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_home)
                    3 -> setDraw(holder.btn, CommunityMaterial.Icon.cmd_timer)
                    4 -> setDraw(holder.btn, CommunityMaterial.Icon.cmd_calendar_text)
                    5 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_grade)
                    6 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_play_circle_filled)
                    7 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_public)
                    8 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_info)
                    9 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_help)
                    11 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_settings)
                    12 -> setDraw(holder.btn, GoogleMaterial.Icon.gmd_play_circle_filled)
                }

                when (stru.icone) {
                    1 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_school
                        ).color(cor)
                    )
                    2 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_home
                        ).color(cor)
                    )
                    3 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            CommunityMaterial.Icon.cmd_timer
                        ).color(cor)
                    )
                    4 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            CommunityMaterial.Icon.cmd_calendar_text
                        ).color(cor)
                    )
                    5 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_grade
                        ).color(cor)
                    )
                    6 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_play_circle_filled
                        ).color(cor)
                    )
                    7 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_public
                        ).color(0xff9E9E9E.toInt())
                    )
                    8 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_info
                        ).color(cor)
                    )
                    9 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_help
                        ).color(cor)
                    )
                    11 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_settings
                        ).color(0xfff44336.toInt())
                    )
                    12 -> holder.image?.setImageDrawable(
                        IconicsDrawable(
                            holder.image.context,
                            GoogleMaterial.Icon.gmd_play_circle_filled
                        ).color(0xfff44336.toInt())
                    )
                }
            }
            is Separador -> {
                holder.title?.text = stru.titulo
            }

            is SeparadorBig -> {
                holder.title?.text = stru.titulo
                holder.subtitle?.text = stru.subtitulo
            }

            is DataProgramacao -> {
                holder.title?.text = stru.titulo
                holder.subtitle?.text = stru.hora
                if (stru.tipo.trim() != "") {
                    holder.image?.setImageDrawable(
                        IconicsDrawable(holder.image.context, GoogleMaterial.Icon.gmd_navigate_next)
                            .color(ContextCompat.getColor(holder.image.context, R.color.ARROW))
                    )
                }
            }

            is Ebd_item -> {
                holder.title?.text = stru.titulo
                holder.subtitle?.text = stru.subtitulo
            }
            is Nucleos_item -> {
                holder.title?.text = stru.nome
                holder.subtitle?.text = stru.hora
            }
            is String -> when (stru) {
                "Loading" -> holder.title?.text = "Carregando núcleos..."
                else -> holder.title?.text = stru
            }
            is escalas_item -> {
                holder.title?.text = stru.titulo
                holder.subtitle?.text = stru.subtitulo
            }
            is SeparadorNovo -> {
                holder.title?.text = stru.titulo
            }
            is NucleosInfo -> {
                holder.title?.text = stru.titulo
                holder.subtitle?.text = holder.subtitle?.resources?.getString(
                    R.string.hour_formatted,
                    stru.hora / 100,
                    String.format("%02d", stru.hora % 100)
                )
                if (stru.extra) {
                    holder.image?.setImageDrawable(
                        IconicsDrawable(holder.image.context, GoogleMaterial.Icon.gmd_navigate_next)
                            .color(ContextCompat.getColor(holder.image.context, R.color.ARROW))
                    )
                }
            }
            is NucleosTitulo -> {
                holder.subtitle?.visibility = View.GONE
                holder.title?.text = stru.titulo
            }
            is oneStringDiff_copy -> {
                holder.subtitle?.visibility = View.GONE
                holder.title?.text = stru.titulo
            }
            is NestedRecycler -> {
                holder.recycler?.adapter = fastAdapter

                val gridLayoutManager = LinearLayoutManager(
                    holder.recycler?.context,
                    LinearLayoutManager.VERTICAL,
                    false
                )

                holder.recycler?.layoutManager = gridLayoutManager
                holder.recycler?.isNestedScrollingEnabled = false
            }
            is oneStringDiff_copy2 -> {
                holder.title?.text = stru.titulo
                holder.image?.setImageDrawable(
                    IconicsDrawable(holder.image.context, GoogleMaterial.Icon.gmd_arrow_forward)
                        .color(ContextCompat.getColor(holder.image.context, R.color.FontWeak))
                )
            }
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.title?.text = null
        holder.subtitle?.text = null
        holder.recycler?.adapter = null
        holder.image?.setImageDrawable(null)
    }

    // Manually create the ViewHolder class
    class ViewHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView? = v.findViewById(R.id.title)
        val subtitle: TextView? = v.findViewById(R.id.subtitle)
        val image: ImageView? = v.findViewById(R.id.imageView)
        val btn: AppCompatButton? = v.findViewById(R.id.menubutton)
        val recycler: RecyclerView? = v.findViewById(R.id.defaultRecycler)
    }
}

// -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //