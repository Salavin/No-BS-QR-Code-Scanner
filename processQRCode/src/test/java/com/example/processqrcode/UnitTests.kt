package com.example.processqrcode

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner


@RunWith(PowerMockRunner::class)
@PrepareForTest(Uri::class)
class UnitTests
{
    @Test
    fun decodeTEXTTest()
    {
        val qrCode = QRCode("Hello! This is a test of a simple string QR code.")
        assertEquals(qrCode.type, QRCodeType.TEXT)
        assertEquals(qrCode.data["Text"], "Hello! This is a test of a simple string QR code.")
        assertEquals(qrCode.intent, null)
        assertEquals(qrCode.string, "Hello! This is a test of a simple string QR code.")
    }

    @Test
    fun decodeURLTest1()
    {
        val qrCode = QRCode("URL:https://samlav.in")
        assertEquals(qrCode.type, QRCodeType.URL)
        assertEquals(qrCode.data["URL"], "https://samlav.in")
    }

    @Test
    fun decodeURLTest2()
    {
        val qrCode = QRCode("URLTO:https://samlav.in")
        assertEquals(qrCode.type, QRCodeType.URL)
        assertEquals(qrCode.data["URL"], "https://samlav.in")
    }

    @Test
    fun decodeURLTest3()
    {
        val qrCode = QRCode("https://samlav.in")
        assertEquals(qrCode.type, QRCodeType.URL)
        assertEquals(qrCode.data["URL"], "https://samlav.in")
    }

    @Test
    fun decodeURLTest4()
    {
        val qrCode = QRCode("http://samlav.in")
        assertEquals(qrCode.type, QRCodeType.URL)
        assertEquals(qrCode.data["URL"], "http://samlav.in")
    }

//    @Test
//    fun decodeEMAILTest1()
//    {
//        val string = "mailto:someone@yoursite.com"
//        PowerMockito.mockStatic(Uri::class.java)
//        val uri: Uri = mock(Uri::class.java)
//        PowerMockito.`when`<Any>(Uri::class.java, "parse", string).thenReturn(Uri.parse(string))
//
//        val qrCode = QRCode(string)
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "someone@yoursite.com")
//    }
//
//    @Test
//    fun decodeEMAILTest2()
//    {
//        val qrCode = QRCode("mailto:someone@yoursite.com?subject=Mail%20from%20Our%20Site")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "someone@yoursite.com")
//        assertEquals(qrCode.data["Subject"], "Mail from Our Site")
//    }
//
//    @Test
//    fun decodeEMAILTest3()
//    {
//        val qrCode =
//            QRCode("mailto:someone@yoursite.com?cc=someoneelse@theirsite.com,another@thatsite.com,me@mysite.com&bcc=lastperson@theirsite.com&subject=Big%20News")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "someone@yoursite.com")
//        assertEquals(qrCode.data["Subject"], "Big News")
//        assertEquals(
//            qrCode.data["Cc"],
//            "someoneelse@theirsite.com,another@thatsite.com,me@mysite.com"
//        )
//        assertEquals(qrCode.data["Bcc"], "lastperson@theirsite.com")
//    }
//
//    @Test
//    fun decodeEMAILTest4()
//    {
//        val qrCode =
//            QRCode("mailto:someone@yoursite.com?cc=someoneelse@theirsite.com,another@thatsite.com,me@mysite.com&bcc=lastperson@theirsite.com&subject=Big%20News&body=Body%20goes%20here.")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "someone@yoursite.com")
//        assertEquals(qrCode.data["Subject"], "Big News")
//        assertEquals(
//            qrCode.data["Cc"],
//            "someoneelse@theirsite.com,another@thatsite.com,me@mysite.com"
//        )
//        assertEquals(qrCode.data["Bcc"], "lastperson@theirsite.com")
//        assertEquals(qrCode.data["Body"], "Body goes here.")
//    }
//
//    @Test
//    fun decodeEMAILTest5()
//    {
//        val qrCode = QRCode("MATMSG:TO:salavin12@gmail.com;;")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "salavin12@gmail.com")
//    }
//
//    @Test
//    fun decodeEMAILTest6()
//    {
//        val qrCode = QRCode("MATMSG:TO:salavin12@gmail.com;SUB:Test;;")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "salavin12@gmail.com")
//        assertEquals(qrCode.data["Subject"], "Test")
//    }
//
//    @Test
//    fun decodeEMAILTest7()
//    {
//        val qrCode = QRCode("MATMSG:TO:salavin12@gmail.com;SUB:Test;BODY:This is a test.;;")
//        assertEquals(qrCode.type, QRCodeType.EMAIL)
//        assertEquals(qrCode.data["Email"], "salavin12@gmail.com")
//        assertEquals(qrCode.data["Subject"], "Test")
//        assertEquals(qrCode.data["Body"], "This is a test.")
//    }

    @Test
    fun decodeTELTest1()
    {
        val qrCode = QRCode("TEL:555-555-5555")
        assertEquals(qrCode.type, QRCodeType.TEL)
        assertEquals(qrCode.data["Number"], "555-555-5555")
    }

    @Test
    fun decodeTELTest2()
    {
        val qrCode = QRCode("TEL:555.555.5555")
        assertEquals(qrCode.type, QRCodeType.TEL)
        assertEquals(qrCode.data["Number"], "555.555.5555")
    }

    @Test
    fun decodeTELTest3()
    {
        val qrCode = QRCode("TEL:5555555555")
        assertEquals(qrCode.type, QRCodeType.TEL)
        assertEquals(qrCode.data["Number"], "5555555555")
    }

    @Test
    fun decodeTELTest4()
    {
        val qrCode = QRCode("tel:5555555555")
        assertEquals(qrCode.type, QRCodeType.TEL)
        assertEquals(qrCode.data["Number"], "5555555555")
    }

    @Test
    fun decodeTELTest5()
    {
        val qrCode = QRCode("TEL:+1-555-555-5555")
        assertEquals(qrCode.type, QRCodeType.TEL)
        assertEquals(qrCode.data["Number"], "+1-555-555-5555")
    }

    @Test
    fun decodeTELTest6()
    {
        val qrCode = QRCode("tel:abcdefg")
        assertEquals(qrCode.type, QRCodeType.TEXT)
        assertEquals(qrCode.data["Number"], "tel:abcdefg [Note: Invalid phone number]")
    }

    @Test
    fun decodeSMSTest1()
    {
        val qrCode = QRCode("sms:5555555555")
        assertEquals(qrCode.type, QRCodeType.SMS)
        assertEquals(qrCode.data["Number"], "5555555555")
    }

    @Test
    fun decodeSMSTest2()
    {
        val qrCode = QRCode("sms:5555555555:This%20is%20my%20text%20message.")
        assertEquals(qrCode.type, QRCodeType.SMS)
        assertEquals(qrCode.data["Number"], "5555555555")
        assertEquals(qrCode.data["Message"], "This is my text message.")
    }

    @Test
    fun decodeCONTACTTest()
    {
        val qrCode =
            QRCode("MECARD:N:Owen,Sean;ADR:76 9th Avenue, 4th Floor, New York, NY 10011;TEL:12125551212;EMAIL:srowen@example.com;;")
        assertEquals(qrCode.type, QRCodeType.CONTACT)
        assertEquals(qrCode.data["Name"], "Sean Owen")
        assertEquals(qrCode.data["Address"], "76 9th Avenue, 4th Floor, New York, NY 10011")
        assertEquals(qrCode.data["Phone"], "12125551212")
        assertEquals(qrCode.data["Email"], "srowen@example.com")
    }

    @Test
    fun decodeGEO1()
    {
        val qrCode = QRCode("geo:40.71872,-73.98905")
        assertEquals(qrCode.type, QRCodeType.GEO)
        assertEquals(qrCode.data["Latitude"], "40.71872")
        assertEquals(qrCode.data["Longitude"], "-73.98905")
    }

    @Test
    fun decodeGEO2()
    {
        val qrCode = QRCode("GEO:40.71872,-73.98905")
        assertEquals(qrCode.type, QRCodeType.GEO)
        assertEquals(qrCode.data["Latitude"], "40.71872")
        assertEquals(qrCode.data["Longitude"], "-73.98905")
    }

    @Test
    fun decodeGEO3()
    {
        val qrCode = QRCode("geo:40.71872,-73.98905,100")
        assertEquals(qrCode.type, QRCodeType.GEO)
        assertEquals(qrCode.data["Latitude"], "40.71872")
        assertEquals(qrCode.data["Longitude"], "-73.98905")
    }

    @Test
    fun decodeWIFITestNoEAP()
    {
        val qrCode = QRCode("WIFI:T:WPA;S:mynetwork;P:mypass;;")
        assertEquals(qrCode.type, QRCodeType.WIFI)
        assertEquals(qrCode.data["Authentication type"], "WPA")
        assertEquals(qrCode.data["SSID"], "mynetwork")
        assertEquals(qrCode.data["Password"], "mypass")
    }

    @Test
    fun decodeWIFITestNoEAPLowercase()
    {
        val qrCode = QRCode("wifi:t:wpa;s:mynetwork;p:mypass;;")
        assertEquals(qrCode.type, QRCodeType.WIFI)
        assertEquals(qrCode.data["Authentication type"], "wpa")
        assertEquals(qrCode.data["SSID"], "mynetwork")
        assertEquals(qrCode.data["Password"], "mypass")
    }

    @Test
    fun decodeWIFITestEAP()
    {
        val qrCode =
            QRCode("WIFI:T:WPA2-EAP;S:mynetwork;P:mypass;H:true;E:TTLS;A:anon;I:myidentity;PH2:MSCHAPV2;;")
        assertEquals(qrCode.type, QRCodeType.WIFI)
        assertEquals(qrCode.data["Authentication type"], "WPA2-EAP")
        assertEquals(qrCode.data["SSID"], "mynetwork")
        assertEquals(qrCode.data["Password"], "mypass")
        assertEquals(qrCode.data["Hidden"], "true")
        assertEquals(qrCode.data["EAP method"], "TTLS")
        assertEquals(qrCode.data["Anonymous identity"], "anon")
        assertEquals(qrCode.data["Identity"], "myidentity")
        assertEquals(qrCode.data["Phase 2 method"], "MSCHAPV2")
    }

    @Test
    fun decodeWIFITestNoSSID()
    {
        val qrCode =
            QRCode("WIFI:T:WPA2-EAP;P:mypass;H:true;E:TTLS;A:anon;I:myidentity;PH2:MSCHAPV2;;")
        assertEquals(qrCode.type, QRCodeType.TEXT)
        assertEquals(
            qrCode.data["Text"],
            "WIFI:T:WPA2-EAP;P:mypass;H:true;E:TTLS;A:anon;I:myidentity;PH2:MSCHAPV2;; [Note: Missing SSID]"
        )
    }

    @Test
    fun decodeWIFITestWEP()
    {
        val qrCode = QRCode("WIFI:T:WEP;S:mynetwork;P:mypass;;")
        assertEquals(qrCode.type, QRCodeType.TEXT)
        assertEquals(
            qrCode.data["Text"],
            "WIFI:T:WEP;S:mynetwork;P:mypass;; [Note: WEP networks are not supported]"
        )
    }
}