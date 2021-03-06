package `in`.samlav.processqrcode

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.provider.Settings
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList

class QRCode(var string: String, var options: QRCodeOptions)
{
    var type: QRCodeType
    var data: MutableMap<String, String> = mutableMapOf()
    var intent: Intent? = null

    init
    {
        val regex = Regex(pattern = "\\A(\\w+):")
        if (regex.containsMatchIn(this.string))
        {
            when (regex.find(this.string)?.groupValues?.get(1)
                ?.lowercase(Locale.ROOT)) //TODO: Implement vEvent and vCard
            {
                "http", "https", "url", "urlto" ->
                {
                    this.type = QRCodeType.URL
                    decodeURL()
                }
                "mailto", "matmsg" ->
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
            decodeTEXT()
        }
    }

    private fun decodeURL()  // Handles market links as well
    {
        when
        {
            this.string.substring(0, 4).lowercase(Locale.ROOT) == "url:" -> this.data["URL"] =
                this.string.substring(4)
            this.string.substring(0, 6).lowercase(Locale.ROOT) == "urlto:" -> this.data["URL"] =
                this.string.substring(6)
            else -> this.data["URL"] = this.string  // http, https, market
        }
        this.intent = Intent(Intent.ACTION_VIEW)
        this.intent!!.data = Uri.parse(this.data["URL"])
    }

    private fun decodeEMAIL()
    {
        if (this.string.substring(0, 7).lowercase(Locale.ROOT) == "mailto:")
        {
            val uri: Uri = Uri.parse(this.string)
            this.data["Email"] = uri.userInfo + '@' + uri.authority
            for (parameter in uri.queryParameterNames)
            {
                @Suppress("DEPRECATION")
                this.data[parameter.lowercase(Locale.ROOT).capitalize(Locale.ROOT)] =
                    uri.getQueryParameter(parameter)!!
            }
            this.intent = Intent(Intent.ACTION_VIEW)
            this.intent!!.data = Uri.parse(this.string)
        }
        else
        {
            this.intent = Intent(Intent.ACTION_SENDTO).apply {
                type = "message/rfc822"
                data = Uri.parse("mailto:")
            }
            this.string = this.string.replace("\n", "%0D%0A")
            val regex = Regex(
                pattern = "(?:(?:TO:)|(?:SUB:)|(?:BODY:))(.*?)(?=(?:;TO:)|(?:;SUB:)|(?:;BODY:)|(?:;\\Z)|(?:;;\\Z)|\\Z)",
                option = RegexOption.IGNORE_CASE)
            var addresses = ""
            var subject = ""
            var body = ""
            for (match in regex.findAll(this.string))
            {
                val matchResult = match.groupValues[1]
                when
                {
                    match.groupValues[0].substring(0, 2).lowercase(Locale.ROOT) == "to" ->
                    {
                        this.data["Email"] = matchResult
                        this.intent?.putExtra(Intent.EXTRA_EMAIL, matchResult)
                        addresses = matchResult
                    }
                    match.groupValues[0].substring(0, 3).lowercase(Locale.ROOT) == "sub" ->
                    {
                        this.data["Subject"] = matchResult
                        this.intent?.putExtra(Intent.EXTRA_SUBJECT, matchResult)
                        subject = matchResult
                    }
                    match.groupValues[0].substring(0, 4).lowercase(Locale.ROOT) == "body" ->
                    {
                        this.data["Body"] = matchResult.replace("%0D%0A", "\n")
                        this.intent?.putExtra(Intent.EXTRA_TEXT, matchResult)
                        body = matchResult
                    }
                }
            }
            this.intent?.data = Uri.parse(String.format("mailto:%s?subject=%s?&body=%s", addresses, subject, body))
        }
    }

    private fun decodeTEL()
    {
        var regex = Regex(
            pattern = "\\A(?:(?:tel:)|(?:sms:)|(?:smsto:)|(?:mms:)|(?:mmsto:))([^:]*):?(.*)\\Z",
            option = RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(this.string) && !containsLetters(regex.find(this.string)!!.groupValues[1]))
        {
            this.data["Number"] = regex.find(this.string)!!.groupValues[1]
        }
        else
        {
            this.type = QRCodeType.TEXT
            if (options.getOption(APPEND_ERROR) == true)
            {
                this.string += " [Note: Invalid phone number]"
            }
            decodeTEXT()
            return
        }
        if (this.type == QRCodeType.SMS)
        {
            this.intent = Intent(Intent.ACTION_SENDTO)
            this.intent!!.data = Uri.parse("smsto:" + this.data["Number"])
            val messageBody = regex.find(this.string)!!.groupValues[2]
            if (messageBody != "")
            {
                val decodedMessageBody = URLDecoder.decode(messageBody, "UTF-8")
                this.data["Message"] = decodedMessageBody
                this.intent!!.putExtra("sms_body", decodedMessageBody)
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
                }
                else
                {
                    if (this.string[i] == '\\' && this.string.length > i + 1 && "\\;,:".contains(
                            this.string[i]
                        )
                    )  // Checking for escaped characters
                    {
                        continue  //Skip over the '\'
                    }
                    else
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
                    when (tmpParam.lowercase(Locale.ROOT))
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
                            if (options.getOption(APPEND_ERROR) == true)
                            {
                                this.string += " [Note: Invalid formatting]"
                            }
                            decodeTEXT()
                            return
                        }
                    }
                    inParameter = true
                    tmpParam = ""
                }
            }
        }
        if (this.data["Name"]?.contains(',') == true)  // "When a field is divided by a comma (,), the first half is treated as the last name and the second half is treated as the first name."
        {
            this.data["Name"] = this.data["Name"]?.substringAfter(',')!! + ' ' + this.data["Name"]?.substringBefore(',')!!
        }
        this.intent = Intent(Intent.ACTION_INSERT)
        this.intent!!.type = ContactsContract.Contacts.CONTENT_TYPE
        for (param in this.data.keys)
        {
            when (param)  //TODO: Find out how to insert other fields
            {
                "Name" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.NAME,
                    this.data[param]
                )
                "Reading" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.PHONETIC_NAME,
                    this.data[param]
                )
                "Phone" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.PHONE,
                    this.data[param]
                )
//                "Video" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.EXTRA_DATA_SET, this.data[param])
                "Email" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.EMAIL,
                    this.data[param]
                )
                "Memo" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.NOTES,
                    this.data[param]
                )
//                "Birthday" -> this.intent!!.putExtra(ContactsContract.Intents.Insert., this.data[param])
                "Address" -> this.intent!!.putExtra(
                    ContactsContract.Intents.Insert.POSTAL,
                    this.data[param]
                )
//                "URL" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NAME, this.data[param])
//                "Nickname" -> this.intent!!.putExtra(ContactsContract.Intents.Insert.NAME, this.data[param])
            }
        }
    }

    private fun decodeGEO()
    {
        val regex = Regex(
            pattern = "geo:(.*?),(.*?)(?:,|\\Z)",
            option = RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(this.string) && (regex.find(this.string)!!.groupValues.size == 3))
        {
            val groupValues = regex.find(this.string)!!.groupValues
            this.data["Latitude"] = groupValues[1]
            this.data["Longitude"] = groupValues[2]
            this.intent = Intent(Intent.ACTION_VIEW)
            this.intent!!.data = Uri.parse(this.string)
        }
        else
        {
            this.type = QRCodeType.TEXT
            decodeTEXT()
        }
    }

//    private fun decodeCAL()  //TODO
//    {
//
//    }

    private fun decodeWIFI()
    {
        val regex = Regex(
            pattern = "(?:(?::)|;)(?:[tspheai]|(?:ph2)):(.*?)(?=;)",
            option = RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(this.string))
        {
            for (matchResult: MatchResult in regex.findAll(this.string))
            {
                when (matchResult.groupValues[0][1].lowercaseChar())  // Grabbing first group, second character
                {
                    't' -> this.data["Authentication type"] = matchResult.groupValues[1]
                    's' -> this.data["SSID"] = matchResult.groupValues[1]
                    'p' ->
                    {
                        if (matchResult.groupValues[0].lowercase(Locale.ROOT).contains("ph2"))
                        {
                            this.data["Phase 2 method"] = matchResult.groupValues[1]
                        } else
                        {
                            this.data["Password"] = matchResult.groupValues[1]
                        }
                    }
                    'h' -> this.data["Hidden"] = matchResult.groupValues[1]
                    'e' -> this.data["EAP method"] = matchResult.groupValues[1]
                    'a' -> this.data["Anonymous identity"] = matchResult.groupValues[1]
                    'i' -> this.data["Identity"] = matchResult.groupValues[1]
                }
            }
        }
        if (!this.data.keys.contains("SSID"))  // This parameter is required
        {
            this.type = QRCodeType.TEXT
            if (options.getOption(APPEND_ERROR) == true)
            {
                this.string += " [Note: Missing SSID]"
            }
            decodeTEXT()
            return  // Aborting WIFI decoding
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
                    when (this.data["Authentication type"]?.lowercase(Locale.ROOT))
                    {
                        "wep" ->
                        {
                            this.type = QRCodeType.TEXT
                            if (options.getOption(APPEND_ERROR) == true)
                            {
                                this.string += " [Note: WEP networks are not supported]"
                            }
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
                            when (this.data["EAP method"]!!.lowercase(Locale.ROOT))
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
                            when (this.data["Phase 2 method"]!!.lowercase(Locale.ROOT))
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
                            if (this.data["Authentication type"]?.lowercase(Locale.ROOT) == "wpa2-eap")
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
                    this.data["Hidden"]?.lowercase(
                        Locale.ROOT
                    ).toBoolean()
                )
            }
        }
        val suggestionsList: MutableList<WifiNetworkSuggestion> = ArrayList()
        suggestionsList.add(wifiNetworkSuggestion.build())
        val bundle = Bundle()
        bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST, suggestionsList as java.util.ArrayList<out Parcelable>)
        this.intent!!.putExtras(bundle)
    }

    private fun decodeTEXT()
    {
        this.data["Text"] = this.string
    }

    private fun containsLetters(string: String): Boolean
    {
        for (c in string)
        {
            if (c in 'A'..'Z' || c in 'a'..'z') {
                return true
            }
        }
        return false
    }
}