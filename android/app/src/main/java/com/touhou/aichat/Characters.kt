package com.touhou.aichat

import com.touhou.aichat.R

data class Character(
    val chineseName: String,
    val englishName: String,
    val drawableRes: Int
)

object Characters {
    val all: List<Character> = listOf(
        Character("博丽灵梦", "Reimu Hakurei", R.drawable.icon_l1_reimu_hakurei),
        Character("雾雨魔理沙", "Marisa Kirisame", R.drawable.icon_l1_marisa_kirisame),
        Character("琪露诺", "Cirno", R.drawable.icon_l1_cirno),
        Character("十六夜咲夜", "Sakuya Izayoi", R.drawable.icon_l1_sakuya_izayoi),
        Character("蕾米莉亚·斯卡蕾特", "Remilia Scarlet", R.drawable.icon_l1_remilia_scarlet),
        Character("芙兰朵露·斯卡蕾特", "Flandre Scarlet", R.drawable.icon_l1_flandre_scarlet),
        Character("东风谷早苗", "Sanae Kochiya", R.drawable.icon_l1_sanae_kochiya),
        Character("魂魄妖梦", "Youmu Konpaku", R.drawable.icon_l1_youmu_konpaku),
        Character("八云紫", "Yukari Yakumo", R.drawable.icon_l1_yukari_yakumo),
        Character("八云蓝", "Ran Yakumo", R.drawable.icon_l1_ran_yakumo),
        Character("橙", "Chen", R.drawable.icon_l1_chen),
        Character("爱丽丝·玛格特洛依德", "Alice Margatroid", R.drawable.icon_l1_alice_margatroid),
        Character("红美铃", "Hong Meiling", R.drawable.icon_l1_hong_meiling),
        Character("帕秋莉·诺蕾姬", "Patchouli Knowledge", R.drawable.icon_l1_patchouli_knowledge),
        Character("小恶魔", "Koakuma", R.drawable.icon_l1_koakuma),
        Character("古明地觉", "Satori Komeiji", R.drawable.icon_l1_satori_komeiji),
        Character("古明地恋", "Koishi Komeiji", R.drawable.icon_l1_koishi_komeiji),
        Character("射命丸文", "Aya Shameimaru", R.drawable.icon_l1_aya_shameimaru),
        Character("八意永琳", "Eirin Yagokoro", R.drawable.icon_l1_eirin_yagokoro),
        Character("藤原妹红", "Fujiwara no Mokou", R.drawable.icon_l1_fujiwara_no_mokou),
        Character("铃仙·优昙华院·因幡", "Reisen Udongein Inaba", R.drawable.icon_l1_reisen_udongein_inaba),
        Character("因幡帝", "Tewi Inaba", R.drawable.icon_l1_tewi_inaba),
        Character("蓬莱山辉夜", "Kaguya Houraisan", R.drawable.icon_l1_kaguya_houraisan),
        Character("洩矢诹访子", "Suwako Moriya", R.drawable.icon_l1_suwako_moriya),
        Character("八坂神奈子", "Kanako Yasaka", R.drawable.icon_l1_kanako_yasaka),
        Character("比那名居天子", "Tenshi Hinanawi", R.drawable.icon_l1_tenshi_hinanawi),
        Character("西行寺幽幽子", "Yuyuko Saigyouji", R.drawable.icon_l1_yuyuko_saigyouji),
        Character("伊吹萃香", "Suika Ibuki", R.drawable.icon_l1_suika_ibuki),
        Character("露米娅", "Rumia", R.drawable.icon_l1_rumia),
        Character("河城荷取", "Nitori Kawashiro", R.drawable.icon_l1_nitori_kawashiro),
    )
}
