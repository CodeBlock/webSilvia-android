package me.elrod.websilviaandroid

import android.app.{ Activity, AlertDialog }
import android.content.{ Context, Intent }
import android.os.Bundle
import android.util.Log
import android.view.{ Menu, MenuInflater, MenuItem, View, Window }
import android.widget.{ ArrayAdapter, ProgressBar, TextView, ScrollView }

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect.IO

import com.google.zxing.integration.android.{ IntentIntegrator, IntentResult }

import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.{ IO => WSIO, Socket }

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
  override def onPostCreate(bundle: Bundle): Unit = {
    super.onPostCreate(bundle)
    setContentView(R.layout.main_activity)
    val integrator = new IntentIntegrator(this)
    integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES)
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
      Log.e("MainActivity", "Ohai!!")
      // At this point, we have a successful scan and we can attempt to connect.
      val List(url, hash) = r.split("#", 2).toList
      val socket = WSIO.socket(url)
      Log.e("MainActivity", "CONNECTING!")
      socket.connect
      Log.e("MainActivity", "CONNECTEDISH!")
      socket.emit("foo", "hi")
    }
    ()
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
