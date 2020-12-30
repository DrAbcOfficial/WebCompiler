package net.drabc.webbspcomplier.coinfig

import net.drabc.webbspcomplier.complier.Compiler

object Config {
    val doneList: MutableList<Compiler> = mutableListOf()
    val waitList: MutableList<Compiler> = mutableListOf()
    var filesLoacation = ""
    var zipLocation: String = ""


    val csgPath = "/home/sc-csg_64"
    val bspPath = "/home/sc-bsp_64"
    val visPath = "/home/sc-vis_64"
    val radPath = "/home/sc-rad_64"
    val wadPath = "/home/svencoop"

    val SMTP_host="a"
    val SMTP_port=123
    val SMTP_auth=true
    val STMP_user="c"
    val STMP_pass="d"
    val SMTP_from="e"
    val SMTP_fromnick="Web Compiler Boss"
}