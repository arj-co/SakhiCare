package com.sakhicare.app.data

data class BirthPlanItem(
    val title: String,
    val description: String,
    var isChecked: Boolean = false
)

object BirthPreparednessChecklist {
    fun getDefaultChecklist(): List<BirthPlanItem> = listOf(
        BirthPlanItem("Designated Delivery Facility", "Identified 24x7 PHC/CHC with functional FRU"),
        BirthPlanItem("Emergency Transport Plan", "108 ambulance contact registered and village meeting point fixed"),
        BirthPlanItem("Identified Blood Donor", "2 compatible blood donors listed in case of PPH"),
        BirthPlanItem("Essential Newborn Care Kit", "Clean clothes, sterile blade, cord tie ready"),
        BirthPlanItem("Accompanying Person", "Identified family member or ASHA to accompany mother")
    )
}
