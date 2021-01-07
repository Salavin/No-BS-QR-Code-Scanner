package com.example.nobsqrcodescanner.qr

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiEnterpriseConfig
import android.os.Bundle
import android.os.Parcelable
import kotlin.collections.ArrayList
import android.net.wifi.WifiNetworkSuggestion
import android.provider.ContactsContract
import android.provider.Settings
import java.util.*

class ProcessQRCode(var string: String)
{
    var type: QRCodeType
    var data: MutableMap<String, String> = mutableMapOf()
    var intent: Intent? = null

    init
    {
        val regex = Regex(pattern = "\\A(\\w+):")
        if (regex.containsMatchIn(this.string))
        {
            when (regex.find(this.string)?.groupValues?.get(1)?.toLowerCase(Locale.ROOT)) //TODO: Implement vEvent and vCard
            {
                "http", "https", "url", "urlto" ->
                {
                    this.type = QRCodeType.URL
                    decodeURL()
                }
                "mailto" ->
                {
                    this.type = QRCodeType.EMAIL
                    decodeEMAIL()
                }
                "tel" ->
                {
                    this.type = QRCodeType.TEL
                    decodeTEL()
                }
                "mecard" ->
                {
                    this.type = QRCodeType.CONTACT
                    decodeCONTACT()
                }
                "sms", "smsto", "mms", "mmsto" ->
                {
                    this.type = QRCodeType.SMS
                    decodeTEL()
                }
                "geo" ->
                {
                    this.type = QRCodeType.GEO
                    decodeGEO()
                }
                "wifi" ->
                {
                    this.type = QRCodeType.WIFI
                    decodeWIFI()
                }
                "market" ->
                {
                    this.type = QRCodeType.MARKET
                    decodeURL()
                }
                else ->
                {
                    this.type = QRCodeType.TEXT
                    decodeTEXT()
                }
            }
        } else
        {
            this.type = QRCodeType.TEXT
        }
    }

    private fun decodeURL()  // Handles market links as well
    {
        when
        {
            this.string.substring(0, 4) == "URL:" -> this.data["URL"] = this.string.substring(4)
            this.string.substring(0, 6) == "URLTO:" -> this.data["URL"] = this.string.substring(6)
            else -> this.data["URL"] = this.string  // http, https, market
        }
        this.intent = Intent(Intent.ACTION_VIEW)
        this.intent!!.data = Uri.parse(this.data["URL"])
    }

    private fun decodeEMAIL()
    {
        if (this.string.substring(0, 7).toLowerCase(Locale.ROOT) == "mailto:")
        {
            val uri: Uri = Uri.parse(this.string)
            this.data["Email"] = uri.userInfo + '@' + uri.authority
            for (parameter in uri.queryParameterNames)
            {
                this.data[parameter.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT)] = uri.getQueryParameter(parameter)!!
            }
        }
        else
        {
            val regex = Regex(pattern = "(?:(?:TO:)|(?:SUB:)|(?:BODY:))(.*?)(?=(?:;TO:)|(?:;SUB:)|(?:;BODY:)|(?:;\\Z)|\\Z)", option = RegexOption.IGNORE_CASE)
            for (match in regex.findAll(this.string))
            {
                when (match.groupValues[0].toLowerCase(Locale.ROOT))
                {
                    "to:" -> this.data["To"] = match.groupValues[1]
                    "sub:" -> this.data["Subject"] = match.groupValues[1]
                    "body:" -> this.data["Body:"] = match.groupValues[1]
                }
            }
        }
        this.intent = Intent(Intent.ACTION_VIEW)
        this.intent!!.data = Uri.parse(this.string)
    }

    private fun decodeTEL()
    {
        var regex = Regex(pattern = "\\A.{4}\\D*(\\d+)+(?:|\\Z)")
        if (regex.containsMatchIn(this.string))
        {
            var number = ""
            for (matchResult: MatchResult in regex.findAll(this.string))
            {
                number += matchResult.groupValues[1]
            }
            this.data["Number"] = number
        } else
        {
            this.type = QRCodeType.TEXT
            decodeTEXT()
        }
        if (this.type == QRCodeType.SMS)
        {
            this.intent = Intent(Intent.ACTION_SENDTO)
            this.intent!!.data = Uri.parse("smsto:" + this.data["Number"])

            regex = Regex(pattern = "\\A.{3}:\\S+:(\\S+)\\Z")
            val result: String? = regex.find(this.string)?.groupValues?.get(1)
            if (result != null)
            {
                this.data["Message"] = result
                this.intent!!.putExtra("sms_body", result)
            }
        }
        else
        {
            this.intent = Intent(Intent.ACTION_DIAL)
            this.intent!!.data = Uri.parse(this.string)
        }
    }

    private fun decodeCONTACT()
    {
        var inParameter = false
        var tmpParam = ""

        for (i in 7 until this.string.length)
        {
            if (inParameter)
            {
                if (this.string[i] == ';' && this.string[i - 1] != '\\')  // Checking to see if there is a non-escaped semicolon, ending the parameter
                {
                    inParameter = false
                } else
                {
                    if (this.string[i] == '\\' && this.string.length > i + 1 && "\\;,:".contains(
                            this.string[i]
                        )
                    )  // Checking for escaped characters
                    {
                        continue  //Skip over the '\'
                    } else
                    {
                        this.data[this.data.keys.last()] =
                            this.data[this.data.keys.last()] + this.string[i]
                    }
                }
            }
            else
            {
                if (this.string[i] != ':')
                {
                    tmpParam += this.string[i]
                }
                else
                {
                    when (tmpParam.toLowerCase(Locale.ROOT))
                    {
                        "n" -> this.data["Name"] = ""
                        "sound" -> this.data["Reading"] = ""
                        "tel" -> this.data["Phone"] = ""
                        "tel-av" -> this.data["Video"] = ""
                        "email" -> this.data["Email"] = ""
                        "note" -> this.data["Memo"] = ""
                        "bday" -> this.data["Birthday"] = ""
                        "adr" -> this.data["Address"] = ""
                        "url" -> this.data["URL"] = ""
                        "nickname" -> this.data["Nickname"] = ""
                        else ->  // Invalid formatting, abort
                        {
                            this.type = QRCodeType.TEXT
                            decodeTEXT()
                            return
                        }
                    }
                    inParameter = true
                }
            }
        }
        this.intent = Intent(Intent.ACTION_INSERT)
        this.intent!!.type = ContactsContract.Contacts.CONTENT_TYPE
        for (param in this.data.keys)
        {
            when (param)  //TODO: Find out how to insert other fields
            {
                "Name" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NAME, this.data[param])
                "Reading" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.PHONETIC_NAME, this.data[param])
                "Phone" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.PHONE, this.data[param])
//                "Video" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.EXTRA_DATA_SET, this.data[param])
                "Email" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.EMAIL, this.data[param])
                "Memo" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NOTES, this.data[param])
//                "Birthday" -> this.intent!!.putExtra(ContactsContract.Intents.Insert., this.data[param])
                "Address" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.POSTAL, this.data[param])
//                "URL" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NAME, this.data[param])
//                "Nickname" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NAME, this.data[param])
            }
        }
    }

    private fun decodeGEO()
    {
        this.intent = Intent(Intent.ACTION_VIEW)
        this.intent!!.data = Uri.parse(this.string)
    }

//    private fun decodeCAL()  //TODO
//    {
//
//    }

    private fun decodeWIFI()
    {
        var inParameter = false

        for (i in 5 until this.string.length)
        {
            if (inParameter)
            {
                if (this.string[i] == ';' && this.string[i - 1] != '\\')  // Checking to see if there is a non-escaped semicolon, ending the parameter
                {
                    inParameter = false
                } else
                {
                    if (this.string[i] == '\\' && this.string.length > i + 1 && "\\;,:".contains(
                            this.string[i]
                        )
                    )  // Checking for escaped characters
                    {
                        continue  //Skip over the '\'
                    } else
                    {
                        this.data[this.data.keys.last()] =
                            this.data[this.data.keys.last()] + this.string[i]
                    }
                }
            }
            else
            {
                if ("tspheai".contains(this.string[i].toLowerCase()))
                {
                    if (this.string[i].toLowerCase() == 'p' && this.string.length > i + 2 && this.string[i + 1].toLowerCase() == 'h' && this.string[i + 2] == '2')
                    {
                        this.data["Phase 2 method"] = ""
                    }
                    else
                    {
                        when (this.string[i].toLowerCase())
                        {
                            't' -> this.data["Authentication type"] = ""
                            's' -> this.data["SSID"] = ""
                            'p' -> this.data["Password"] = ""
                            'h' -> this.data["Hidden"] = ""
                            'e' -> this.data["EAP method"] = ""
                            'a' -> this.data["Anonymous identity"] = ""
                            'i' -> this.data["Identity"] = ""
                        }
                    }
                    inParameter = true
                }
                else  // Invalid formatting, abort
                {
                    this.type = QRCodeType.TEXT
                    decodeTEXT()
                    return
                }
            }
        }
        if (!this.data.keys.contains("SSID"))  // This parameter is required
        {
            this.type = QRCodeType.TEXT
            decodeTEXT()
            return
        }
        this.intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
        val wifiNetworkSuggestion: WifiNetworkSuggestion.Builder = WifiNetworkSuggestion.Builder()
        for (key in this.data.keys)
        {
            when (key)
            {
                "SSID" -> this.data[key]?.let { wifiNetworkSuggestion.setSsid(it) }
                "Password" ->
                {
                    when (this.data["Authentication type"]?.toLowerCase(Locale.ROOT))
                    {
                        "wep" ->
                        {
                            this.type = QRCodeType.TEXT
                            this.string += " [Note: WEP networks are not supported]"
                            decodeTEXT()
                            return  // Aborting WIFI decoding
                        }
                        "wpa", "wpa2" -> this.data["Password"]?.let {
                            wifiNetworkSuggestion.setWpa2Passphrase(
                                it
                            )
                        }  // WPA doesn't seem to be supported, but from what I can tell it is backwards compatible with WPA2
                        "wpa2-eap", "wpa3-eap" ->
                        {
                            val wifiEnterpriseConfig = WifiEnterpriseConfig()
                            when (this.data["EAP method"]!!.toLowerCase(Locale.ROOT))
                            {
                                "aka" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.AKA
                                "aka_prime" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.AKA_PRIME
                                "none" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.NONE
                                "peap" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.PEAP
                                "pwd" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.PWD
                                "sim" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.SIM
                                "tls" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.TLS
                                "ttls" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.TTLS
                                "unauth_tls" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.UNAUTH_TLS
                                "wapi_cert" -> wifiEnterpriseConfig.eapMethod =
                                    WifiEnterpriseConfig.Eap.WAPI_CERT
                            }
                            when (this.data["Phase 2 method"]!!.toLowerCase(Locale.ROOT))
                            {
                                "aka" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.AKA
                                "aka_prime" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.AKA_PRIME
                                "gtc" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.GTC
                                "mschap" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.MSCHAP
                                "mschapv2" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.MSCHAPV2
                                "none" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.NONE
                                "pap" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.PAP
                                "sim" -> wifiEnterpriseConfig.phase2Method =
                                    WifiEnterpriseConfig.Phase2.SIM
                            }
                            wifiEnterpriseConfig.identity = this.data["Identity"]!!
                            wifiEnterpriseConfig.anonymousIdentity =
                                this.data["Anonymous identity"]!!
                            wifiEnterpriseConfig.password = this.data["Password"]!!
                            if (this.data["Authentication type"]?.toLowerCase(Locale.ROOT) == "wpa2-eap")
                            {
                                wifiNetworkSuggestion.setWpa2EnterpriseConfig(wifiEnterpriseConfig)
                            } else
                            {
                                wifiNetworkSuggestion.setWpa3EnterpriseConfig(wifiEnterpriseConfig)
                            }
                        }
                        "wpa3" -> this.data["Password"]?.let {
                            wifiNetworkSuggestion.setWpa3Passphrase(
                                it
                            )
                        }
                    }
                }
                "Hidden" -> wifiNetworkSuggestion.setIsHiddenSsid(
                    this.data["Hidden"]?.toLowerCase(
                        Locale.ROOT
                    ).toBoolean()
                )
            }
        }
        val suggestionsList: MutableList<WifiNetworkSuggestion> = ArrayList()
        suggestionsList.add(wifiNetworkSuggestion.build())
        val bundle: Bundle = Bundle()
        bundle.putParcelableArrayList(
            Settings.EXTRA_WIFI_NETWORK_LIST,
            suggestionsList as java.util.ArrayList<out Parcelable>
        )
        this.intent!!.putExtras(bundle)
    }

    private fun decodeTEXT()
    {
        this.data["Text"] = this.string
    }
}

enum class QRCodeType
{
    TEXT, URL, EMAIL, TEL, CONTACT, SMS, GEO, CAL, WIFI, MARKET
}

// https://github.com/zxing/zxing/wiki/Barcode-Contents