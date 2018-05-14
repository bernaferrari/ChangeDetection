package com.example.changedetection.forms

import android.text.InputType
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon

object FormConstants {

    private var i = 0
    val isection = ++i
    val iname = ++i
    val imemberfromipc = ++i
    val iphone = ++i
    val iemail = ++i
    val iaddress = ++i
    val ibirthday = ++i
    val isons = ++i
    val imember = ++i
    val ispouse = ++i
    val ichurch = ++i
    val iselectnucleo = ++i

    val imeetingscheduledate = ++i
    val ipreletor = ++i
    val url = ++i
    val isinglephone = ++i

    val serverNames = mutableMapOf<Int, String>().apply {
        this[FormConstants.iphone] = "phone"
        this[FormConstants.iaddress] = "address"
        this[FormConstants.ispouse] = "spouse"
        this[FormConstants.ibirthday] = "birthday"
        this[FormConstants.isons] = "sons"
        this[FormConstants.ichurch] = "church"
        this[FormConstants.iemail] = "email"
        this[FormConstants.iname] = "name"
        this[FormConstants.iselectnucleo] = "participant"
        this[FormConstants.imemberfromipc] = "ismemberfromipc"
        this[FormConstants.imeetingscheduledate] = "timestamp"
        this[FormConstants.ipreletor] = "preacher"
        this[FormConstants.isinglephone] = "phonecontact"
        this[FormConstants.url] = "url"
    }

    val hintNames = mutableMapOf<Int, String>().apply {
        this[FormConstants.iphone] = "digite o telefone"
        this[FormConstants.iaddress] = "digite o endereço"
        this[FormConstants.ispouse] = "digite o nome do cônjuge"
        this[FormConstants.ibirthday] = "selecione o aniversário"
        this[FormConstants.isons] = "digite o nome dos filhos"
        this[FormConstants.ichurch] = "selecione a igreja"
        this[FormConstants.iemail] = "digite o email"
        this[FormConstants.iname] = "digite o nome"
        this[FormConstants.iselectnucleo] = "participant"
        this[FormConstants.imemberfromipc] = "é membro da igreja?"
        this[FormConstants.imeetingscheduledate] = "selecione a data"
        this[FormConstants.ipreletor] = "digite o preletor"
        this[FormConstants.isinglephone] = "digite o telefone"

        this[FormConstants.url] = "type the address"
    }

    fun inputType (kind: Int): Int{
        return when (kind){
            url -> InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    val reversedserverNames = mutableMapOf<String, Int>().apply {
        this["phone"] = FormConstants.iphone
        this["address"] = FormConstants.iaddress
        this["spouse"] = FormConstants.ispouse
        this["birthday"] = FormConstants.ibirthday
        this["sons"] = FormConstants.isons
        this["church"] = FormConstants.ichurch
        this["email"] = FormConstants.iemail
        this["name"] = FormConstants.iname
        this["participant"] = FormConstants.iselectnucleo
        this["ismemberfromipc"] = FormConstants.imemberfromipc
        this["timestamp"] = FormConstants.imeetingscheduledate
        this["preacher"] = FormConstants.ipreletor
        this["host"] = FormConstants.url
        this["phonecontat"] = FormConstants.isinglephone
    }

    val iconArr2 = mutableMapOf<Int, IIcon>().apply {
        this[FormConstants.iaddress] = CommunityMaterial.Icon.cmd_map
        this[FormConstants.ispouse] = CommunityMaterial.Icon.cmd_human_male_female
        this[FormConstants.ibirthday] = CommunityMaterial.Icon.cmd_cake
        this[FormConstants.ichurch] = CommunityMaterial.Icon.cmd_ticket
        this[FormConstants.iemail] = CommunityMaterial.Icon.cmd_email
        this[FormConstants.iphone] = CommunityMaterial.Icon.cmd_whatsapp
        this[FormConstants.isons] = CommunityMaterial.Icon.cmd_human_child
        this[FormConstants.imember] = CommunityMaterial.Icon.cmd_church
        this[FormConstants.iselectnucleo] = CommunityMaterial.Icon.cmd_home
        this[FormConstants.imemberfromipc] = CommunityMaterial.Icon.cmd_church
        this[FormConstants.imeetingscheduledate] = CommunityMaterial.Icon.cmd_calendar_today
        this[FormConstants.ipreletor] = CommunityMaterial.Icon.cmd_microphone_variant
        this[FormConstants.isinglephone] = CommunityMaterial.Icon.cmd_whatsapp

        this[FormConstants.url] = GoogleMaterial.Icon.gmd_web
        this[FormConstants.iname] = GoogleMaterial.Icon.gmd_title

    }

    val subtitletrue = mutableMapOf<Int, String>().apply {
        this[FormConstants.iaddress] = "É obrigatório o endereço completo."
        this[FormConstants.ibirthday] = "É obrigatório a data de nascimento."
        this[FormConstants.iphone] = "É obrigatório um telefone."
    }

    val subtitlefalse = mutableMapOf<Int, String>().apply {
        this[FormConstants.iaddress] = "É obrigatório o bairro."
        this[FormConstants.ibirthday] = "É obrigatório a data de nascimento."
        this[FormConstants.iphone] = "É obrigatório um telefone."
//        this[FormConstants.iname] = "É obrigatório o rua."
    }

    val colorArr2 = hashMapOf<Int, Int>().apply {
        this[FormConstants.iaddress] = 0xff009688.toInt()
        this[FormConstants.ispouse] = 0xff673AB7.toInt()
        this[FormConstants.ibirthday] = 0xff3F51B5.toInt()
        this[FormConstants.ichurch] = 0xff3F51B5.toInt()
        this[FormConstants.iemail] = 0xff3F51B5.toInt()
        this[FormConstants.iphone] = 0xff3F51B5.toInt()
        this[FormConstants.iname] = 0xff3F51B5.toInt()
        this[FormConstants.isons] = 0xff3F51B5.toInt()
        this[FormConstants.imember] = 0xff3F51B5.toInt()
        this[FormConstants.iselectnucleo] = 0xff3F51B5.toInt()
        this[FormConstants.imeetingscheduledate] = 0xff3F51B5.toInt()
    }

}