package me.elrod.websilviaandroid

import android.app.{ Activity, PendingIntent }
import android.content.{ Intent, IntentFilter }
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep

import scala.language.implicitConversions // lolscala

class AnnoyingNFCStuff(activity: Activity) {
  val nfc = NfcAdapter.getDefaultAdapter(activity.getApplicationContext)

  val intent = PendingIntent.getActivity(
    activity,
    0,
    new Intent(
      activity,
      activity.getClass)
    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
    0)

    val ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
    ndef.addDataType("*/*")
    val intentFiltersArray = Array(ndef)
    val techListsArray = Array(Array(classOf[IsoDep].getName))

    def enableForeground(): Unit =
      nfc.enableForegroundDispatch(activity, intent, intentFiltersArray, techListsArray)

    def disableForeground(): Unit = nfc.disableForegroundDispatch(activity)
}
