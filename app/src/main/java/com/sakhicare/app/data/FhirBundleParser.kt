package com.sakhicare.app.data

import org.json.JSONObject

object FhirBundleParser {
    fun extractPatientName(fhirJson: String): String? {
        return try {
            val root = JSONObject(fhirJson)
            val entries = root.optJSONArray("entry") ?: return null
            for (i in 0 until entries.length()) {
                val resource = entries.getJSONObject(i).optJSONObject("resource") ?: continue
                if (resource.optString("resourceType") == "Patient") {
                    val nameArr = resource.optJSONArray("name") ?: continue
                    return nameArr.getJSONObject(0).optString("text")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
