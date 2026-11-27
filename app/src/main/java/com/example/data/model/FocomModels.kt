package com.example.data.model

data class FocomContent(
    val newsList: List<FocomNews> = emptyList(),
    val rightsList: List<FocomRight> = emptyList(),
    val delegatesList: List<FocomDelegate> = emptyList(),
    val publicationsList: List<FocomPublication> = emptyList()
)

data class FocomNews(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val date: String = "",
    val category: String = "Actualité", // ex: "NAO", "CSE", "Vie Syndicale", "Accords"
    val isHotState: Boolean = false,
    val imageUrl: String? = null,
    val webUrl: String? = null
)

data class FocomRight(
    val id: String = "",
    val title: String = "",
    val category: String = "Législation", // ex: "Télétravail", "Congés", "Rémunération", "Santé"
    val summary: String = "",
    val detailMarkdown: String = "",
    val focomAdvice: String = ""
)

data class FocomDelegate(
    val id: String = "",
    val name: String = "",
    val role: String = "", // ex: "Délégué Syndical", "Représentant de Proximité", "Élu CSE"
    val entity: String = "", // ex: "Centrapel", "Free SAS", "Protelco", "MCRA", "Iliad"
    val region: String = "National", // ex: "Paris (Siège)", "Bordeaux", "Marseille", "National"
    val email: String = "",
    val phone: String = "",
    val avatarLetters: String = "",
    val photoResName: String? = null
)

data class FocomPublication(
    val id: String = "",
    val title: String = "",
    val type: String = "Publication", // "Journal", "Tract", "Affiche"
    val date: String = "",
    val thumbnailUrl: String? = null,
    val pdfUrl: String = "",
    val excerpt: String = "",
    val content: String = ""
)


data class FocomFlashBanner(
    val id: String,
    val message: String,
    val type: String,
    val link: String?,
    val startDate: String,
    val endDate: String
)
