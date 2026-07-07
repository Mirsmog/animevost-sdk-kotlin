package com.animevost.sdk.model

data class CatalogLink(
    val title: String,
    val url: String,
    val path: String,
)

data class NavigationData(
    val genres: List<CatalogLink> = emptyList(),
    val types: List<CatalogLink> = emptyList(),
    val years: List<CatalogLink> = emptyList(),
    val sections: List<CatalogLink> = emptyList(),
)

data class CatalogFilter(
    val path: String? = null,
)
