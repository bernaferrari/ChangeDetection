package com.example.changedetection

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.changedetection.util.FAUIUtils
import com.example.changedetection.util.TextDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger


enum class STATUS {
    FULLACCESS, HEADER, DESCRIPTION, PRESENTE, AUSENTE, NONE, STARRED
}

class AcessoItem(name: String, id: String, status: STATUS) : primitiveClassType(name, id, status, 2)

class AcessoHeader(name: String, status: STATUS) : primitiveClassType(name, "", status, 1)

class ChamadaItem(name: String, status: STATUS, id: String = "") :
    primitiveClassType(name, id, status, 2)

class ChamadaHeader(name: String, status: STATUS) : primitiveClassType(name, status, 1)

class LiturgiaItem(
    name: String,
    var subtitle: String,
    var kind: String,
    timestamp: Long,
    var isVisible: Boolean = false
) :
    primitiveClassType(name, "", timestamp, 0)

class AllLiturgiasItem(
    timestamp: Long,
    name: String,
    var people_that_came: Int,
    var preletor: String,
    id: String
) : primitiveClassType(name, id, timestamp, 2)


class AllMeetingsHeader(name: String) : primitiveClassType(name, 1)

class AllMeetingsItem(
    timestamp: Long,
    name: String,
    var diffSize: Int,
    var url: String
) : primitiveClassType(name, url, timestamp, 2)

class AllMeetingsItemNotBeingUsed(timestamp: Long, name: String, var trueid: String) :
    primitiveClassType(name, trueid, timestamp, 2)

class miniSeparator(name: String, prio: Int) : primitiveClassType(name, "", STATUS.NONE, prio)

class EBDItemmm(
    name: String,
    var teacher: String,
    var room: String,
    id: String,
    prio: Int = 3,
    val isAdmin: Boolean = false
) :
    primitiveClassType(name, id, 1, prio)

class Nested(prio: Int) : primitiveClassType("", "", 0, prio)

class EbdItem(name: String, id: String, val subtitle: String = "") : NucleosItem(name, id, 5, 1)

open class NucleosItem(name: String, id: String, weekday: Long, prio: Int) :
    primitiveClassType(name, id, weekday, STATUS.NONE, prio)

class NucleosHeader(name: String, weekday: Long, prio: Int) :
    primitiveClassType(name, "", weekday, STATUS.NONE, prio)

class NucleosMore(name: String, weekday: Long, prio: Int) :
    primitiveClassType(name, "", weekday, STATUS.NONE, prio)

class FastDescription(name: String, weekday: Long, prio: Int) :
    primitiveClassType(name, "", weekday, prio)

class FlatButton(name: String, weekday: Long, prio: Int) :
    primitiveClassType(name, "", weekday, STATUS.NONE, prio)

class ButtonBiblaLibras(name: String, id: String, prio: Int) :
    primitiveClassType(name, id, -1, STATUS.PRESENTE, prio)

class moreBasicItem2(
    name: String,
    id: String,
    timestamp: Long,
    val kind: Int = 0,
    var hour: Int = 0,
    prio: Int,
    val isAdmin: Boolean = false
) :
    primitiveClassType(name, id, timestamp, STATUS.NONE, prio)

class moreBasicHeader2(
    name: String,
    id: String,
    timestamp: Long,
    val kind: Int = 0,
    val hour: Int = 0,
    prio: Int
) :
    primitiveClassType(name, id, timestamp, STATUS.NONE, prio)

class pesquisarHospedeiro(
    name: String,
    id: String,
    val address: String
) :
    primitiveClassType(name, id, 0, STATUS.NONE, 0)

class pesquisarPreletor(
    name: String,
    id: String,
    val address: String
) :
    primitiveClassType(name, id, 0, STATUS.NONE, 0)


open class primitiveClassType(
    var name: String,
    var id: String = "",
    var timestamp: Long = 0,
    var status: STATUS,
    var prio: Int = 9
) {
    // TODOS ENCONTROS
    constructor(name: String, id: String, timestamp: Long, prio: Int) : this(
        name,
        id,
        timestamp,
        STATUS.NONE,
        prio
    )

    constructor(name: String, prio: Int) : this(name, "", 0, STATUS.NONE, prio)

    // CHAMADA
    constructor(name: String, id: String, status: STATUS, prio: Int) : this(
        name,
        id,
        0,
        status,
        prio
    )

    constructor(name: String, status: STATUS, prio: Int) : this(name, "", 0, status, prio)
}

class NucleosAdapter(stru: primitiveClassType) : PrimitiveAbstractItem(stru) {

    override fun getType(): Int = when (stru) {
        is EbdItem -> 22
        is NucleosItem -> 9
        else -> 99
    }

    override fun getViewHolder(v: View): RecyclerView.ViewHolder =
        PrimitiveAbstractItem.ViewHolder(v)

    override fun getLayoutRes(): Int {
        return when (stru) {
            is EbdItem -> R.layout.ebd_item
            is NucleosItem -> R.layout.nuc_nuc_item

            else -> return R.layout.item_sticky_generic3
        }
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        if (holder is PrimitiveAbstractItem.ViewHolder) {
            when (stru) {
                is EbdItem -> {
                    holder.nome.text = stru.name
                    holder.subtitle?.text = holder.subtitle?.resources?.getString(
                        R.string.hour_formatted,
                        stru.prio / 100,
                        String.format("%02d", stru.prio % 100)
                    )
                }
                is NucleosItem -> {
                    holder.nome.text = stru.name
                    holder.subtitle?.text = holder.subtitle?.resources?.getString(
                        R.string.hour_formatted,
                        stru.prio / 100,
                        String.format("%02d", stru.prio % 100)
                    )
                }
            }
        }
    }
}


open class PrimitiveAbstractItem(val stru: primitiveClassType) :
    AbstractItem<PrimitiveAbstractItem, RecyclerView.ViewHolder>() {
    val fastAdapter: FastItemAdapter<PrimitiveAbstractItem> = FastItemAdapter()

    override fun getType(): Int = when (stru) {
        is AllMeetingsHeader -> 0
        is AllMeetingsItem -> 1
        is AllMeetingsItemNotBeingUsed -> 2
        is ChamadaItem -> 3
        is ChamadaHeader -> 4
        is AcessoItem -> 5
        is AcessoHeader -> 6
        is miniSeparator -> 7
        is Nested -> 8

        is EbdItem -> 22
        is NucleosItem -> 9

        is NucleosHeader -> 10
        is NucleosMore -> 11
        is FastDescription -> 12

        is EBDItemmm -> 13
        is FlatButton -> 14

        is LiturgiaItem -> 15
        is AllLiturgiasItem -> 16
        is ButtonBiblaLibras -> 17

        is moreBasicItem2 -> 18
        is moreBasicHeader2 -> 19

        is pesquisarHospedeiro -> 20
        is pesquisarPreletor -> 21

        else -> 99
    }

    override fun getViewHolder(v: View): RecyclerView.ViewHolder {
        return when (stru) {
            is ChamadaItem -> ViewHolder1(v)
//            is EBDItemmm -> ViewHolder3(v)
            is moreBasicItem2, is EBDItemmm -> ViewHolder3(v)
            is FlatButton -> ButtonHolder(v)
            is Nested -> ViewHolder2(v)
            is LiturgiaItem -> LiturgiaViewHolder(v)
            is pesquisarHospedeiro -> pesquisarHospedeiroViewHolder(v)
            is pesquisarPreletor -> pesquisarPreletorViewHolder(v)
            else -> PrimitiveAbstractItem.ViewHolder(v)
        }
    }

    override fun getLayoutRes(): Int {
        return when (stru) {
            is AllMeetingsHeader -> R.layout.big_title
            is AllMeetingsItem -> R.layout.todos_encontros_item
            is AllMeetingsItemNotBeingUsed -> R.layout.todos_encontros_item
            is ChamadaItem -> R.layout.item_chamada
            is ChamadaHeader -> R.layout.big_title
            is AcessoItem -> R.layout.item_acesso
            is AcessoHeader -> R.layout.item_simple_card
            is miniSeparator -> R.layout.main_separador_programacao
            is Nested -> R.layout.nested

            is EbdItem -> R.layout.nuc_ebd_item

            is NucleosItem -> R.layout.nuc_nuc_item
            is NucleosHeader -> R.layout.big_title_without_margin
            is NucleosMore -> R.layout.item_nucleo_more
            is FastDescription -> R.layout.item_para_participar

            is FlatButton -> R.layout.flat_button
            is LiturgiaItem -> R.layout.liturgiaitem
            is AllLiturgiasItem -> R.layout.todos_encontros_item

            is ButtonBiblaLibras -> R.layout.item_principal_culto

            is moreBasicItem2, is EBDItemmm -> R.layout.ebd_item
            is moreBasicHeader2 -> R.layout.item_sticky_generic3

            is pesquisarHospedeiro -> R.layout.hospedeiro_dialog_item
            is pesquisarPreletor -> R.layout.preletor_dialog_item
            else -> return R.layout.item_sticky_generic3
        }
    }

    override fun bindView(holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        if (stru is moreBasicHeader2) {
//            holder.title?.text = stru.name
//        } else if (stru is moreButton) {
//            holder.btn?.text = stru.name
//            if (stru.name == "Administradores") {
//                holder.btn?.setTextColor(0xff000000.toInt())
//            }
//            setDraw(holder.btn, GoogleMaterial.Icon.gmd_exit_to_app)
//            holder.btn.
        }


        if (holder is pesquisarHospedeiroViewHolder && stru is pesquisarHospedeiro) {
            holder.title.text = stru.name
            holder.subtitle.text = stru.address
        }

        if (holder is pesquisarPreletorViewHolder && stru is pesquisarPreletor) {
            holder.title.text = stru.name
        }

        if (holder is PrimitiveAbstractItem.ViewHolder) {
            when (stru) {
                is AllMeetingsItem -> {
                    holder.nome.text = stru.name
                }
                is AllMeetingsHeader -> {
                    holder.nome.text = stru.name
                }
                is AllMeetingsItemNotBeingUsed -> {
                    holder.nome.text = stru.name
                }
                is AcessoItem -> {
                    holder.nome.text = stru.name
                    if (stru.name.isEmpty()) {
                        holder.nome.visibility = View.GONE
                    }
                    Logger.d("Nome: " + stru.name + " --- id: " + stru.id)
                    holder.subtitle?.text = stru.id

                }
                is AcessoHeader -> {
                    holder.nome.text = stru.name
                }
                is ChamadaHeader -> {
                    holder.nome.text = stru.name
                }
                is miniSeparator -> {
                    holder.nome.text = stru.name
                }
                is EbdItem -> {
                    holder.nome.text = stru.name
                    holder.subtitle?.text = stru.subtitle
                }
                is NucleosItem -> {
                    holder.nome.text = stru.name
                    holder.subtitle?.text = holder.subtitle?.resources?.getString(
                        R.string.hour_formatted,
                        stru.prio / 100,
                        String.format("%02d", stru.prio % 100)
                    )
                }
                is NucleosHeader -> {
                    holder.nome.text = stru.name
                }
                is NucleosMore -> {
                    holder.nome.text = stru.name
                    holder.imgpos?.setImageDrawable(
                        IconicsDrawable(
                            holder.imgpos.context,
                            GoogleMaterial.Icon.gmd_arrow_forward
                        )
                            .color(ContextCompat.getColor(holder.imgpos.context, R.color.FontWeak))
                    )
                }
                is FastDescription -> {
                    holder.nome.text = stru.name
                }
                is ButtonBiblaLibras -> {
                    holder.nome.text = stru.name

                    holder.imgpos?.setImageDrawable(getDrawableee(stru.id, holder.imgpos.context))
                }
            }
        } else if (holder is ButtonHolder && stru is FlatButton) {
            holder.btn?.text = stru.name
            holder.btn?.stateListAnimator = null

            if (stru.name == "Administradores") {
                holder.btn?.setTextColor(0xff000000.toInt())
            } else if (stru.name == "Cronograma") {
                holder.btn?.setTextColor(0xff000000.toInt())
            }
        }

        if (holder is LiturgiaViewHolder && stru is LiturgiaItem) {
            holder.title.text = stru.name
            holder.subtitle.text = stru.subtitle

            val iconic = IconicsDrawable(holder.titleimg.context)
            iconic.icon(CommunityMaterial.Icon.cmd_bible)

            val test = arrayOf(
                "Hino",
                "Cântico",
                "Oração",
                "Leitura",
                "Palavra",
                "Outro"
            )

            val icons = arrayOf(
                CommunityMaterial.Icon.cmd_piano,
                CommunityMaterial.Icon.cmd_music_note,
                CommunityMaterial.Icon.cmd_cloud,
                CommunityMaterial.Icon.cmd_bible,
                CommunityMaterial.Icon.cmd_account,
                CommunityMaterial.Icon.cmd_dots_horizontal
            )

            val index = test.indexOfFirst { stru.kind == it }
            if (index > -1) {
                iconic.icon(icons[index])
            }
            iconic.color(ContextCompat.getColor(holder.titleimg.context, R.color.FontStrong))
            holder.titleimg.setImageDrawable(iconic)

            if (!stru.isVisible) {
                holder.up.visibility = View.GONE
                holder.down.visibility = View.GONE
            } else {
                holder.up.visibility = View.VISIBLE
                holder.down.visibility = View.VISIBLE
            }

            holder.up.setImageDrawable(
                IconicsDrawable(holder.up.context).icon(CommunityMaterial.Icon.cmd_arrow_up).color(
                    ContextCompat.getColor(holder.up.context, R.color.FontStrong)
                )
            )

            holder.down.setImageDrawable(
                IconicsDrawable(holder.down.context).icon(CommunityMaterial.Icon.cmd_arrow_down).color(
                    ContextCompat.getColor(holder.down.context, R.color.FontStrong)
                )
            )
        }

        if (holder is PrimitiveAbstractItem.ViewHolder1 && stru is ChamadaItem) {
            holder.nome.text = stru.name

            if (stru.name != "") {
                holder.colorfulimageview.setImageDrawable(
                    TextDrawable.builder().round().build(
                        stru.name[0].toString(),
                        ContextCompat.getColor(holder.star.context, R.color.bolinhaNomeCurto)
                    )
                )
            }

            if (stru.status == STATUS.AUSENTE) {
                holder.star.setImageDrawable(
                    IconicsDrawable(holder.star.context).icon(
                        GoogleMaterial.Icon.gmd_close
                    ).color(0xfff44336.toInt())
                )
            } else {
                holder.star.setImageDrawable(
                    IconicsDrawable(holder.star.context).icon(
                        GoogleMaterial.Icon.gmd_done
                    ).color(0xff4CAF50.toInt())
                )
            }

            holder.info.setImageDrawable(
                IconicsDrawable(holder.info.context).icon(GoogleMaterial.Icon.gmd_info_outline).color(
                    ContextCompat.getColor(holder.info.context, R.color.md_grey_600)
                )
            )

            val stateListDrawable = FAUIUtils.getSelectableBackground(
                holder.star.context,
                ContextCompat.getColor(holder.star.context, R.color.highlight),
                true
            )
            holder.view.background = stateListDrawable
        } else if (holder is PrimitiveAbstractItem.ViewHolder3) {
            var isAdmin = false

            when (stru) {
                is EBDItemmm -> {
                    isAdmin = stru.isAdmin
                    holder.nome.text = stru.name
                    holder.subtitle.text = holder.subtitle.resources?.getString(
                        R.string.ebdclass,
                        stru.room,
                        stru.teacher
                    )
                }
                is moreBasicItem2 -> {
                    isAdmin = stru.isAdmin
                    holder.nome.text = stru.name
//                holder.subtitle.text = holder.subtitle.resources?.getString(
//                    R.string.day_and_hour_formatted,
//                    ActivityNucleos.dayoftheweek(stru.timestamp.toInt()),
//                    stru.hour / 100,
//                    String.format("%02d", stru.hour % 100)
//                )
                }
            }


            if (isAdmin) {
                holder.star.visibility = View.VISIBLE
                holder.separator.visibility = View.VISIBLE

                if (stru.status == STATUS.STARRED) {
                    holder.star.setImageDrawable(
                        IconicsDrawable(
                            holder.star.context,
                            GoogleMaterial.Icon.gmd_star
                        ).color(0xffffc107.toInt())
                    )
                } else {
                    holder.star.setImageDrawable(
                        IconicsDrawable(
                            holder.star.context,
                            GoogleMaterial.Icon.gmd_star_border
                        ).color(0xffffc107.toInt())
                    )
                }

                holder.info.setImageDrawable(
                    IconicsDrawable(holder.info.context).icon(GoogleMaterial.Icon.gmd_info_outline).color(
                        ContextCompat.getColor(holder.info.context, R.color.md_grey_600)
                    )
                )
            } else {
                holder.star.visibility = View.GONE
                holder.separator.visibility = View.GONE
            }
        }

        if (holder is PrimitiveAbstractItem.ViewHolder2) {
            holder.recycler?.adapter = fastAdapter
            val gridLayoutManager =
                LinearLayoutManager(holder.recycler?.context, LinearLayoutManager.VERTICAL, false)
            holder.recycler?.layoutManager = gridLayoutManager
            holder.recycler?.isNestedScrollingEnabled = false
        }
    }

    fun getDrawableee(str: String, context: Context): IconicsDrawable {
        return when (stru.id) {
            "exit" -> {
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_set_none)
                    .color(ContextCompat.getColor(context, R.color.white))
            }
            "crono" -> {
                IconicsDrawable(context, GoogleMaterial.Icon.gmd_chrome_reader_mode)
                    .color(ContextCompat.getColor(context, R.color.FontWeak))
            }
            "admin" -> {
                IconicsDrawable(context, GoogleMaterial.Icon.gmd_supervisor_account)
                    .color(ContextCompat.getColor(context, R.color.FontWeak))
            }
            else -> {
                IconicsDrawable(context, GoogleMaterial.Icon.gmd_chrome_reader_mode)
                    .color(ContextCompat.getColor(context, R.color.FontWeak))
            }
        }
    }

    // Manually create the ViewHolder class
    class ViewHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val nome: TextView = v.findViewById(R.id.title)
        internal val subtitle: TextView? = v.findViewById(R.id.subtitle)
        internal val txtpos: TextView? = v.findViewById(R.id.textView3)
        internal val imgpos: ImageView? = v.findViewById(R.id.imageView)
    }

    internal class ViewHolder1 constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val nome: TextView = v.findViewById(R.id.subtitle)
        internal val colorfulimageview: ImageView = v.findViewById(R.id.colorfulimageview)
        internal val star: ImageView = v.findViewById(R.id.star)
        internal val info: ImageView = v.findViewById(R.id.info)
        internal var view: View = v
    }

    internal class ViewHolder3 constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val nome: TextView = v.findViewById(R.id.title)
        internal val subtitle: TextView = v.findViewById(R.id.subtitle)
//        internal val colorfulimageview: ImageView = v.findViewById(R.id.colorfulimageview)

        internal val separator: View = v.findViewById(R.id.separator)
        internal val star: ImageView = v.findViewById(R.id.info)
        internal val info: ImageView = v.findViewById(R.id.star)
    }

    internal class ViewHolder2 constructor(v: View) : RecyclerView.ViewHolder(v) {
        val recycler: RecyclerView? = v.findViewById(R.id.defaultRecycler)
    }

    internal class ButtonHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val title: TextView? = v.findViewById(R.id.title)
        internal val subtitle: TextView? = v.findViewById(R.id.subtitle)
        val btn: AppCompatButton? = v.findViewById(R.id.menubutton)
    }

    internal class pesquisarHospedeiroViewHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val title: TextView = v.findViewById(R.id.title)
        internal val subtitle: TextView = v.findViewById(R.id.subtitle)
    }

    internal class pesquisarPreletorViewHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val title: TextView = v.findViewById(R.id.title)
    }

    internal class LiturgiaViewHolder constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal val title: TextView = v.findViewById(R.id.title)
        internal val subtitle: TextView = v.findViewById(R.id.subtitle)
        internal val titleimg: ImageView = v.findViewById(R.id.titleimg)
        internal val up: ImageView = v.findViewById(R.id.up)
        internal val down: ImageView = v.findViewById(R.id.down)
    }
}