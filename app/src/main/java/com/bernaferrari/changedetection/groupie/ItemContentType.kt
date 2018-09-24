package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_content_type.*

/**
 * Used to create a simple line chart.
 */
class ItemContentType(
    private val contentType: String,
    private val count: Int,
    val remove: ((String) -> (Unit)),
    val onClick: ((String) -> (Unit))
) : Item() {

    override fun getLayout(): Int {
        return R.layout.item_content_type
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.containerView.context

        viewHolder.remove.setImageDrawable(
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete)
                .colorRes(R.color.FontWeak)
                .sizeDp(20)
        )

        viewHolder.subtitle.text =
                context.resources.getQuantityString(R.plurals.items, count, count)
        viewHolder.title.text = contentType

        when {
            contentType == "application/pdf" -> {
                viewHolder.icon.setImageDrawable(
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_file_pdf)
                        .colorRes(R.color.md_red_500)
                )
            }
            contentType.split("/").firstOrNull() == "image" -> {
                viewHolder.icon.setImageDrawable(
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_file_image)
                        .colorRes(R.color.md_green_500)
                )
            }
            contentType == "text/html" -> {
                viewHolder.icon.setImageDrawable(
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_file_document)
                        .colorRes(R.color.md_blue_500)
                )
            }
            else -> {
                viewHolder.icon.setImageDrawable(
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_file)
                        .colorRes(R.color.md_grey_500)
                )
            }
        }

        viewHolder.remove.setOnClickListener {
            remove.invoke(contentType)
        }

        viewHolder.itemView.setOnClickListener {
            onClick.invoke(contentType)
        }

    }
}

