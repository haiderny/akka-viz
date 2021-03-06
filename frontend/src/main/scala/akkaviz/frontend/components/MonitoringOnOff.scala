package akkaviz.frontend.components

import org.scalajs.dom.{Event, _}
import rx.{Ctx, Var}

import scalatags.JsDom.all._

class MonitoringOnOff(status: Var[MonitoringStatus]) extends Component with OnOffWithLabel {

  inp.onchange = (d: Event) => {
    status() = Awaiting(Synced(inp.checked))
  }

  val statusTrigger = status.foreach {
    case UnknownYet =>
      lbl.innerHTML = "Awaiting server status"
      inp.disabled = true

    case Awaiting(s) =>
      lbl.innerHTML = s"Awaiting server confirmation for $s"
      inp.disabled = true
      inp.checked = s.asBoolean

    case synced: Synced =>
      inp.checked = synced.asBoolean
      inp.disabled = false
      lbl.innerHTML = s"Monitoring is <b>$synced</b>"

  }

  override def attach(parent: Element): Unit = {
    val elem = Seq[Frag](
      div(`class` := "panel-body", stateBtn)
    ).render

    parent.appendChild(elem)
  }
}

sealed trait MonitoringStatus

sealed trait Synced extends MonitoringStatus {
  def asBoolean: Boolean
}

object Synced {
  def apply(b: Boolean) = if (b) On else Off
}

case object On extends Synced {
  val asBoolean = true
}

case class Awaiting(target: Synced) extends MonitoringStatus

case object Off extends Synced {
  val asBoolean = false
}

case object UnknownYet extends MonitoringStatus
