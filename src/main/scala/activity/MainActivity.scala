package me.elrod.websilviaandroid

import android.app.{ Activity, AlertDialog }
import android.content.{ Context, Intent }
import android.nfc.{ NfcAdapter, Tag }
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.view.{ Menu, MenuInflater, MenuItem, View, Window }
import android.widget.{ ArrayAdapter, ProgressBar, TextView, Toast }

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect.IO

import com.google.zxing.integration.android.{ IntentIntegrator, IntentResult }

import io.socket.{ IOAcknowledge, IOCallback, SocketIO, SocketIOException }

import com.google.gson.{ JsonElement, JsonObject } /* UGH! */

import scala.language.implicitConversions // lolscala

object Implicits {
  implicit def toRunnable[F](f: => F): Runnable = new Runnable() {
    def run(): Unit = {
      f
      ()
    }
  }
}

import Implicits._

class MainActivity extends Activity with TypedViewHolder {

  lazy val nfcForegroundUtil = new AnnoyingNFCStuff(this)
  // yeah, this is shit, but it needs to be readable by the intent handler…
  var s: Option[SocketIO] = None
  var isodep: Option[IsoDep] = None // so is this

  private def startQRScanner(): Unit = {
    new IntentIntegrator(this).initiateScan(IntentIntegrator.QR_CODE_TYPES)
  }

  override def onPostCreate(bundle: Bundle): Unit = {
    super.onPostCreate(bundle)
    setContentView(R.layout.main_activity)
    startQRScanner
    ()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater: MenuInflater = getMenuInflater
    inflater.inflate(R.menu.options, menu);
    true
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, intent: Intent): Unit = {
    val result = Option(IntentIntegrator.parseActivityResult(
      requestCode, resultCode, intent))
      .flatMap(e => Option(e.getContents))
      .filter(_.contains("#"))
    result.map { r =>

      // At this point, we have a successful scan and we can attempt to connect.
      val splitUrl = r.split("#")

      if (splitUrl.length < 2) {
        Toast.makeText(
          MainActivity.this,
          "Invalid QR code -- try again!",
          Toast.LENGTH_LONG).show()
        startQRScanner
      } else {
        val url = splitUrl(0)
        val hash = splitUrl(1)

        val socket = new SocketIO(url + "/irma")

        socket.connect(new IOCallback() {
          override def onMessage(json: JsonElement, ack: IOAcknowledge): Unit = {
            new AlertDialog.Builder(MainActivity.this)
              .setTitle("Message from server!")
              .setMessage(json.toString)
              .show
            ()
          }

          override def onMessage(data: String, ack: IOAcknowledge): Unit = {
            new AlertDialog.Builder(MainActivity.this)
              .setTitle("Message from server!")
              .setMessage(data)
              .show
            ()
          }

          override def onError(err: SocketIOException): Unit =
            err.printStackTrace

          override def onConnect(): Unit = { }

          private def handleConnected(): Unit = {
            val j = new JsonObject
            j.addProperty("connID", hash)
            socket.emit("login", j)
            ()
          }

          private def closeSuccess(): Unit = {
            runOnUiThread(
              Toast.makeText(
                MainActivity.this,
                "Finished. You can move your card now.",
                Toast.LENGTH_LONG).show())
          }

          override def onDisconnect(): Unit = { }
          override def on(event: String, ack: IOAcknowledge, args: JsonElement*): Unit = {
            Log.d("MainActivity", s"Received ${event} event with args: ${args.toString}")
            event match {
              case "connected"    => handleConnected
              case "loggedin"     => readyForSwipe(socket)
              case "card_request" => handleCardRequest(socket, args)
              case "finished"     => closeSuccess
              case x              => Log.d("MainActivity", s"Received unhandled $x message.")
            }
            ()
          }
        })
        s = Some(socket) // yeah, this is shit
        ()
      }

        Toast.makeText(
          this,
          "Touch your IRMA card to your phone and HOLD it there until told otherwise.",
          Toast.LENGTH_LONG).show()
    }
    ()
  }

  def sendToCard(bytes: Array[Byte]): Option[Array[Byte]] =
    isodep.map(_.transceive(bytes))

  def handleCardRequest(socket: SocketIO, args: Seq[JsonElement]): Unit = {
    val request = args.headOption.map(_.getAsJsonObject.get("data").getAsString)

    request.map { r =>
      val bytes = Hex.hexStringToBytes(r)
      bytes.map { b =>
        val responseOpt = sendToCard(b)
          .map(_.map("%02X".format(_)).mkString)
        Log.d("MainActivity", "Card response: " + responseOpt.get)
        responseOpt.map { resp =>
          val j = new JsonObject
          j.addProperty("data", resp)
          Log.d("MainActivity", "emitting card_response with data: " + resp)
          s.map(_.emit("card_response", j))
        }
      }
    }
    ()
  }

  def readyForSwipe(s: SocketIO): Unit = {
    isodep.map(_ => s.emit("card_connected", new JsonObject))
    nfcForegroundUtil.enableForeground
    // Do some UI thing to show "ready for card swipe"
  }

  override def onNewIntent(intent: Intent): Unit = {
    val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    isodep = Some(IsoDep.get(tag))
    isodep.map(_.setTimeout(10000))
    isodep.map(_.connect)
    Log.d("MainActivity", "emitting card_connected")
    s.map(_.emit("card_connected", new JsonObject))

    new AlertDialog.Builder(this)
      .setTitle("Swipe successful!")
      .setMessage(tag.toString)
      .show
    ()
  }

  override def onPause(): Unit = {
    super.onPause
    nfcForegroundUtil.disableForeground
  }

  override def onResume(): Unit = {
    super.onResume
    nfcForegroundUtil.enableForeground

    if (!nfcForegroundUtil.nfc.isEnabled) {
      Toast.makeText(
        this,
        "Please activate NFC and press Back to return to the application!",
        Toast.LENGTH_LONG).show()
        startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.about => {
        val b = new AlertDialog.Builder(this)
          .setTitle("About webSilvia-android")
          .setMessage("(c) 2014 Ricky Elrod. Powered by WebSilvia.")
          .show
      }
      case _ => ()
    }
    true
  }
}
