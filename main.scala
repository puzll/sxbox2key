import scala.swing._
import scala.swing.event._
import scala.swing.Swing.pair2Dimension
import java.nio.file._
import java.nio.ByteBuffer
import java.nio.ByteOrder
import Implicits.Fixsizable
import scala.collection.JavaConverters._
import scala.collection.mutable
import java.awt.Robot
import java.awt.KeyboardFocusManager
import scala.xml._

object KeyDialog extends Dialog {

  title = "Key Reader"
  modal = true
  var key = Key.Undefined

  peer.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.empty.asJava)
  peer.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Set.empty.asJava)
  peer.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, Set.empty.asJava)
  peer.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, Set.empty.asJava)

  contents = new Label("Press a key") {
    border = Swing.EmptyBorder(40, 40, 40, 40)
    focusable = true
    listenTo(keys)
    reactions += {
      case KeyPressed(_, pressedkey, _, _) =>
        key = pressedkey
        close()
    }
  }
}

object Main extends SimpleSwingApplication {

  System.loadLibrary("SXB2KJsEvDev")
  if (JsEvDev.openPipe() < 0) throw new Exception

  val jsSelect = new ComboBox(Joystick.names) {
    maximumSize = (Int.MaxValue, preferredSize.height)
  }

  val (buttBars, buttButtons) = {
    for ((code, _) <- Mapping.buttons) yield {
      val butt = new Button(".")
      val bar = new ProgressBar
      butt.reactions += {
        case ButtonClicked(_) =>
          KeyDialog.open()
          Mapping.butt2Key(code, KeyDialog.key)
          butt.text = KeyDialog.key.toString
      }
      bar.max = 1
      (code -> bar, code -> butt)
    }
  }.unzip match { case (a, b) => (a.toMap, b.toMap) }

  val (axesBars, axesButtons) = {
    for ((code, pos, _) <- Mapping.axes) yield {
      val butt = new Button(".")
      val bar = new ProgressBar
      butt.reactions += {
        case ButtonClicked(_) =>
          KeyDialog.open()
          Mapping.axis2Key(code, pos, KeyDialog.key)
          butt.text = KeyDialog.key.toString
      }
      ((code, pos) -> bar, (code, pos) -> butt)
    }
  }.unzip match { case (a, b) => (a.toMap, b.toMap) }


  val jst = new JsThread((evtype: Short, code: Short, value: Int) =>
    Swing.onEDT {
      evtype match {
        case JsEvDev.EV_KEY =>
          for (bar <- buttBars.get(code)) bar.value = value
        case JsEvDev.EV_ABS =>
          for (bar <- axesBars.get(code, true)) bar.value = 0 max value
          for (bar <- axesBars.get(code, false)) bar.value = 0 max -value
        case _ =>
      }
    }
  )

  def onJsSelect(index: Int) {
    for ((code, pos, _) <- Mapping.axes)
      axesBars(code, pos).max =
        if (pos)
          Joystick.infos(index).axes(code)._2
        else
          -Joystick.infos(index).axes(code)._1
    jst.start(Joystick.infos(index))
  }

  if (Joystick.infos.nonEmpty) onJsSelect(jsSelect.selection.index)

  listenTo(jsSelect.selection)
  reactions += {
    case SelectionChanged(_) => onJsSelect(jsSelect.selection.index)
  }

  def top = new MainFrame {
    title = "XBox2Key"

    val contGap = 10
    val relGap = 5
    val unrelGap = 10

    contents = new BoxPanel(Orientation.Vertical) {
      border = Swing.EmptyBorder(contGap, contGap, contGap, contGap)
      contents += jsSelect
      contents += Swing.VStrut(unrelGap)
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new BoxPanel(Orientation.Vertical) {
          for ((code, bname) <- Mapping.buttons) {
            contents += Swing.VStrut(1)
            contents += new BoxPanel(Orientation.Horizontal) {
              val bar = buttBars(code)
              val butt = buttButtons(code)
              val h = butt.preferredSize.height-3
              maximumSize = (Int.MaxValue, h)
              bar.allSizes = (h, h)
              butt.allSizes = (4*h, h)
              contents += Swing.HGlue
              contents += new Label(bname)
              contents += Swing.HStrut(relGap)
              contents += bar
              contents += Swing.HStrut(relGap)
              contents += butt
            }
          }
          contents += Swing.VGlue
        }
        contents += Swing.HStrut(unrelGap)
        contents += new BoxPanel(Orientation.Vertical) {
          for ((code, pos, aname) <- Mapping.axes) {
            contents += Swing.VStrut(1)
            contents += new BoxPanel(Orientation.Horizontal) {
              val bar = axesBars(code, pos)
              val butt = axesButtons(code, pos)
              val h = butt.preferredSize.height-3
              maximumSize = (Int.MaxValue, h)
              bar.allSizes = (4*h, h)
              butt.allSizes = (4*h, h)
              contents += Swing.HGlue
              contents += new Label(aname)
              contents += Swing.HStrut(relGap)
              contents += bar
              contents += Swing.HStrut(relGap)
              contents += butt
            }
          }
          contents += Swing.VGlue
        }
      }
    }

    override def closeOperation() {
      jst.stop()
      JsEvDev.closePipe()
      Mapping.save()
      super.closeOperation()
    }
  }
}

object Mapping {

  val buttons = Array(
    (JsEvDev.BTN_A, "Button A"),
    (JsEvDev.BTN_B, "Button B"),
    (JsEvDev.BTN_X, "Button X"),
    (JsEvDev.BTN_Y, "Button Y"),
    (JsEvDev.BTN_TL, "Button LB"),
    (JsEvDev.BTN_TR, "Button RB"),
    (JsEvDev.BTN_SELECT, "Button Back"),
    (JsEvDev.BTN_START, "Button Start"),
    (JsEvDev.BTN_THUMBL, "Button Left Stick"),
    (JsEvDev.BTN_THUMBR, "Button Right Stick"),
    (JsEvDev.BTN_MODE, "Button Mode")
  )

  val axes = Array(
    (JsEvDev.ABS_X, false, "Left Stick Left"),
    (JsEvDev.ABS_X, true, "Left Stick Right"),
    (JsEvDev.ABS_Y, false, "Left Stick Up"),
    (JsEvDev.ABS_Y, true, "Left Stick Down"),
    (JsEvDev.ABS_RX, false, "Right Stick Left"),
    (JsEvDev.ABS_RX, true, "Right Stick Right"),
    (JsEvDev.ABS_RY, false, "Right Stick Up"),
    (JsEvDev.ABS_RY, true, "Right Stick Down"),
    (JsEvDev.ABS_Z, true, "Left Trigger"),
    (JsEvDev.ABS_RZ, true, "Right Trigger"),
    (JsEvDev.ABS_HAT0X, false, "DPad Left"),
    (JsEvDev.ABS_HAT0X, true, "DPad Right"),
    (JsEvDev.ABS_HAT0Y, false, "DPad Up"),
    (JsEvDev.ABS_HAT0Y, true, "DPad Down")
  )

  private val butt = mutable.Map.empty[Int, Key.Value]
  private val axis = mutable.Map.empty[(Int, Boolean), Key.Value]

  def butt2Key(code: Int): Option[Key.Value] = butt.synchronized { butt.get(code) }
  def butt2Key(code: Int, key: Key.Value): Unit = butt.synchronized { butt(code) = key }
  def axis2Key(code: Int, pos: Boolean): Option[Key.Value] = axis.synchronized { axis.get((code, pos)) }
  def axis2Key(code: Int, pos: Boolean, key: Key.Value): Unit = axis.synchronized { axis((code, pos)) = key }

  def save() {
    val node =
    <sxbox2key>
      <profile>
        <mapping>
        {
          for ((code, key) <- butt) yield
            <button>
              <code>{code}</code>
              <key>{key}</key>
            </button>
        }
        </mapping>
      </profile>
    </sxbox2key>
    val pp = new PrettyPrinter(100, 2)
    Files.write(Paths.get(System.getProperty("user.home")).resolve(".sxbox2key"), pp.format(node).getBytes)
  }
}

class JsThread(callback: (Short, Short, Int) => Unit) {
  private var info: Joystick.Info = null
  @volatile
  private var cont = true
  private val axesValues = mutable.Map.empty[Int, Int]
  private val robot = new Robot

  private def chkbnd(oldval: Int, newval: Int, bnd: Int, key: Key.Value) {
    if (oldval <= bnd) {
      if (newval > bnd) robot.keyPress(key.id)
    } else {
      if (newval <= bnd) robot.keyRelease(key.id)
    }
  }

  private def processEvent(evtype: Short, evcode: Short, evvalue: Int) {
    evtype match {
      case JsEvDev.EV_KEY =>
        for (key <- Mapping.butt2Key(evcode))
          if (evvalue == 0)
            robot.keyRelease(key.id)
          else
            robot.keyPress(key.id)
      case JsEvDev.EV_ABS =>
        val oldval = axesValues.getOrElse(evcode, 0)
        axesValues(evcode) = evvalue
        for ((min, max) <- info.axes.get(evcode)) {
          for (key <- Mapping.axis2Key(evcode, false)) chkbnd(-oldval, -evvalue, -min >> 1, key)
          for (key <- Mapping.axis2Key(evcode, true)) chkbnd(oldval, evvalue, max >> 1, key)
        }
      case _ =>
    }
    if (evtype > 0)
      callback(evtype, evcode, evvalue)
  }

  private def read(js: Joystick) {
    val buf = ByteBuffer.allocateDirect(JsEvDev.INPUT_EVENT_SIZE<<10)
    buf.order(ByteOrder.nativeOrder)
    while (cont) {
      val cnt = js.read(buf)
      if (cnt < 0) throw new Exception
      buf.rewind()
      while (buf.position < cnt) {
        buf.position(buf.position + JsEvDev.INPUT_EVENT_TYPE_OFFSET);
        processEvent(buf.getShort(), buf.getShort(), buf.getInt())
      }
    }
  }

  private val thread = new Thread {
    override def run() {
      Joystick.withFilename(info.filename)(read)
    }
  }

  def start(info: Joystick.Info) {
    stop()
    this.info = info
    axesValues.clear()
    thread.start()
  }

  def stop() {
    cont = false
    JsEvDev.cancelOn()
    thread.join()
    JsEvDev.cancelOff()
    cont = true
  }
}

object Implicits {
  implicit class Fixsizable(c: Component) {
    def allSizes: Dimension = throw new Exception
    def allSizes_=(d: Dimension) {
      c.minimumSize = d;
      c.preferredSize = d;
      c.maximumSize = d;
    }
  }
}

object Joystick {

  type AxesInfo = Map[Int, (Int, Int)]
  class Info(val filename: String, val axes: AxesInfo)

  def withFilename[T](filename: String)(body: Joystick => T): Option[T] = {
    val fd = JsEvDev.open(filename)
    if (fd >= 0)
      try { Some(body(new Joystick(fd))) }
      finally { JsEvDev.close(fd) }
    else
      None
  }

  val (names, infos) =
    (for {
      file <- Files.newDirectoryStream(Paths.get("/dev/input")).asScala
      if (file.getFileName.toString matches "event[0-9]+")
      result <- withFilename(file.toString) { js =>
        for (axesInfo <- js.checkXBox())
          yield (s"${js.getName()} (${file.getFileName})", new Info(file.toString, axesInfo))
      }.flatten
    } yield result).toSeq.unzip

  def testBit(bits: Array[Byte], index: Int): Boolean = ((bits(index>>3) >> (index&0x7)) & 1) != 0
}

class Joystick private (fd: Long) {
  import JsEvDev._
  import Joystick._

  def getName(): String = JsEvDev.getName(fd)

  def getTypeBits(): Array[Byte] = {
    val bits = new Array[Byte](EV_CNT>>3)
    getBits(fd, 0, bits)
    bits
  }

  def getKeyBits(): Array[Byte] = {
    val bits = new Array[Byte](KEY_CNT>>3)
    JsEvDev.getBits(fd, EV_KEY, bits)
    bits
  }

  def getAbsBits(): Array[Byte] = {
    val bits = new Array[Byte](ABS_CNT>>3)
    getBits(fd, EV_ABS, bits)
    bits
  }

  def getAbsInfo(code: Short): Array[Int] = {
    val info = new Array[Int](6)
    getAbs(fd, code, info)
    info
  }

  def read(buf: ByteBuffer): Long = JsEvDev.read(fd, buf)

  def checkXBox(): Option[AxesInfo] = {
    val typeBits = getTypeBits()
    val axes: AxesInfo = if (testBit(typeBits, EV_ABS)) {
      val absBits = getAbsBits()
      val axes = Array[Short](ABS_X, ABS_Y, ABS_Z, ABS_RX, ABS_RY, ABS_RZ, ABS_HAT0X, ABS_HAT0Y)
      (for {
        axis <- axes
        if testBit(absBits, axis)
        info = getAbsInfo(axis)
        min = info(1)
        max = info(2)
        if (max > 0)
      } yield axis.toInt -> (min, max)).toMap
    } else {
      Map.empty
    }
    val buttons = testBit(typeBits, EV_KEY) && {
      val keyBits = getKeyBits()
      val buttons = Array[Short](BTN_A, BTN_B, BTN_X, BTN_Y, BTN_TL, BTN_TR, BTN_SELECT, BTN_START, BTN_MODE, BTN_THUMBL, BTN_THUMBR)
      buttons exists (testBit(keyBits, _))
    }
    if (buttons || axes.nonEmpty)
      Some(axes)
    else
      None
  }
}
