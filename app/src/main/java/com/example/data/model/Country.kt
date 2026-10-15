package com.example.data.model

data class Country(
    val name: String,
    val flagEmoji: String,
    val code: String,
    val dialCode: String
)

object AfricanCountries {
    val list: List<Country> = listOf(
        Country("Cameroun", "🇨🇲", "CM", "+237"),
        Country("Côte d'Ivoire", "🇨🇮", "CI", "+225"),
        Country("Sénégal", "🇸🇳", "SN", "+221"),
        Country("Rép. Dém. du Congo (RDC)", "🇨🇩", "CD", "+243"),
        Country("Congo Brazzaville", "🇨🇬", "CG", "+242"),
        Country("Gabon", "🇬🇦", "GA", "+241"),
        Country("Bénin", "🇧🇯", "BJ", "+229"),
        Country("Togo", "🇹🇬", "TG", "+228"),
        Country("Mali", "🇲🇱", "ML", "+223"),
        Country("Burkina Faso", "🇧🇫", "BF", "+226"),
        Country("Guinée", "🇬🇳", "GN", "+224"),
        Country("Niger", "🇳🇪", "NE", "+227"),
        Country("Tchad", "🇹🇩", "TD", "+235"),
        Country("Madagascar", "🇲🇬", "MG", "+261"),
        Country("Rwanda", "🇷🇼", "RW", "+250"),
        Country("Burundi", "🇧🇮", "BI", "+257"),
        Country("Centrafrique", "🇨🇫", "CF", "+236"),
        Country("Mauritanie", "🇲🇷", "MR", "+222"),
        Country("Maroc", "🇲🇦", "MA", "+212"),
        Country("Algérie", "🇩🇿", "DZ", "+213"),
        Country("Tunisie", "🇹🇳", "TN", "+216"),
        Country("Égypte", "🇪🇬", "EG", "+20"),
        Country("Ghana", "🇬🇭", "GH", "+233"),
        Country("Nigeria", "🇳🇬", "NG", "+234"),
        Country("Kenya", "🇰🇪", "KE", "+254"),
        Country("Angola", "🇦🇴", "AO", "+244"),
        Country("Afrique du Sud", "🇿🇦", "ZA", "+27"),
        Country("Guinée Équatoriale", "🇬🇶", "GQ", "+240"),
        Country("Djibouti", "🇩🇯", "DJ", "+253"),
        Country("Comores", "🇰🇲", "KM", "+269"),
        Country("Sierra Leone", "🇸🇱", "SL", "+232"),
        Country("Libéria", "🇱🇷", "LR", "+231"),
        Country("Tanzanie", "🇹🇿", "TZ", "+255"),
        Country("Ouganda", "🇺🇬", "UG", "+256"),
        Country("Éthiopie", "🇪🇹", "ET", "+251"),
        Country("Mozambique", "🇲🇿", "MZ", "+258"),
        Country("Zambie", "🇿🇲", "ZM", "+260"),
        Country("Zimbabwe", "🇿🇼", "ZW", "+263"),
        Country("Cap-Vert", "🇨🇻", "CV", "+238"),
        Country("Maurice", "🇲🇺", "MU", "+230"),
        Country("Seychelles", "🇸🇨", "SC", "+248"),
        Country("Guinée-Bissau", "🇬🇼", "GW", "+245"),
        Country("Gambie", "🇬🇲", "GM", "+220"),
        Country("Soudan", "🇸🇩", "SD", "+249"),
        Country("Soudan du Sud", "🇸🇸", "SS", "+211")
    ).sortedBy { it.name }

    fun default(): Country = list.first { it.code == "CM" }
}
