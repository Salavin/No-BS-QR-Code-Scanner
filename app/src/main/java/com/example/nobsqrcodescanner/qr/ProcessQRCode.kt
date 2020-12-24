package com.example.nobsqrcodescanner.qr

import java.util.*

class ProcessQRCode(val string: String)
{
    var type: QRCodeType
    lateinit var data: MutableMap<String, String>

    init
    {
        val regex = Regex(pattern = "\\A(\\w+):")
        if (regex.containsMatchIn(string))
        {
            when (regex.find(string).toString()
                .toLowerCase(Locale.ROOT)) //TODO: Implement vEvent and vCard
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
            this.string.substring(0, 4) == "URL:" -> this.data["url"] = this.string.substring(4)
            this.string.substring(0, 6) == "URLTO:" -> this.data["url"] = this.string.substring(6)
            else -> this.data["url"] = this.string  // http, https, market
        }
    }

    private fun decodeEMAIL()
    {
        this.data["email"] = this.string.substring(7)
    }

    private fun decodeTEL()
    {
        var regex = Regex(pattern = "\\A.{4}\\D*(\\d+)+(?:|\\Z)")
        if (regex.containsMatchIn(this.string))
        {
            var number = ""
            for (matchResult: MatchResult in regex.findAll(this.string))
            {
                number += matchResult.value
            }
            this.data["number"] = number
        } else
        {
            this.type = QRCodeType.TEXT
            decodeTEXT()
        }
        if (this.type == QRCodeType.SMS)
        {
            regex = Regex(pattern = "\\A.{3}:\\S+:(\\S+)\\Z")
            this.data["message"] = regex.find(this.string).toString()
        }
    }

    private fun decodeCONTACT()
    {

    }

    private fun decodeGEO()
    {
        val regex = Regex(pattern = "\\A.{4}(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val matches = regex.findAll(this.string)
        if (matches.count() == 2) // Should only be two results exactly
        {
            val latitude = matches.elementAt(0).value
            val longitude = matches.elementAt(1).value
            this.data = mutableMapOf("latitude" to latitude, "longitude" to longitude)
        } else
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
            } else
            {
                if ("tspheai".contains(this.string[i].toLowerCase()))
                {
                    this.data[this.string[i].toUpperCase().toString()] = ""
                    inParameter = true
                }
            }
        }
    }

    private fun decodeTEXT()
    {
        this.data["text"] = this.string
    }
}

enum class QRCodeType
{
    TEXT, URL, EMAIL, TEL, CONTACT, SMS, GEO, CAL, WIFI, MARKET
}