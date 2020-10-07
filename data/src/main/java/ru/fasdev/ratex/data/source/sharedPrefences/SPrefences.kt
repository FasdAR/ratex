package ru.fasdev.ratex.data.source.sharedPrefences

import android.content.SharedPreferences

public class SPrefences (val sPref: SharedPreferences)
{
    companion object {
        const val NAME_SETTINGS = "ratex_shared_pref"
        const val BASE_CURRENCY_CODE = "ratex_bcc"
    }

    var baseCurrencyCode: String?
        get() {
            return sPref.getString(BASE_CURRENCY_CODE, null)
        }
        set(value) {
            sPref.edit().putString(BASE_CURRENCY_CODE, value).apply()
        }
}