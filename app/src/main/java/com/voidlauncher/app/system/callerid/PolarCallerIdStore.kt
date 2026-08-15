package com.voidlauncher.app.system.callerid

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PolarCallerIdRow(
    val number: String,
    val name: String,
    val known: Boolean,
    val at: Long
)

object PolarCallerIdStore {
    private const val Prefs = "polar_caller_id"
    private const val Key = "rows"

    fun add(context: Context, number: String, name: String, known: Boolean) {
        val next = listOf(PolarCallerIdRow(number, name, known, System.currentTimeMillis())) + load(context)
        save(context, next.take(120))
    }

    fun load(context: Context): List<PolarCallerIdRow> {
        val raw = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        PolarCallerIdRow(
                            number = o.optString("number"),
                            name = o.optString("name"),
                            known = o.optBoolean("known"),
                            at = o.optLong("at")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun save(context: Context, rows: List<PolarCallerIdRow>) {
        val arr = JSONArray()
        rows.forEach { row ->
            arr.put(
                JSONObject()
                    .put("number", row.number)
                    .put("name", row.name)
                    .put("known", row.known)
                    .put("at", row.at)
            )
        }
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
            .edit()
            .putString(Key, arr.toString())
            .apply()
    }
}
