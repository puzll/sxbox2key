import scala.swing._
import scala.swing.event._
import scala.swing.Swing.pair2Dimension
import java.nio.file._
import java.nio.ByteBuffer
import java.nio.ByteOrder
import Implicits.Fixsizable
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.collection.mutable
import java.awt.Robot

object KeyDialog extends Dialog {
  title = "Key Reader"
  modal = true
  var key = Key.Undefined
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

  val (jnames, jinfos) = Joystick.scanEvDir().unzip

  val butt2Key = mutable.Map.empty[Int, Key.Value]
  val axis2Key = mutable.Map.empty[(Int, Boolean), Key.Value]

  lazy val top = new MainFrame {
    title = "XBox2Key"

    val contGap = 10
    val relGap = 5
    val unrelGap = 10

    val buttons = Array(
      ("Button A", JsEvDev.BTN_A),
      ("Button B", JsEvDev.BTN_B),
      ("Button X", JsEvDev.BTN_X),
      ("Button Y", JsEvDev.BTN_Y),
      ("Button LB", JsEvDev.BTN_TL),
      ("Button RB", JsEvDev.BTN_TR),
      ("Button Back", JsEvDev.BTN_SELECT),
      ("Button Start", JsEvDev.BTN_START),
      ("Button Left Stick", JsEvDev.BTN_THUMBL),
      ("Button Right Stick", JsEvDev.BTN_THUMBR),
      ("Button Mode", JsEvDev.BTN_MODE)
    )

    val axes = Array[(String, Short, Boolean)](
      ("Left Stick Left", JsEvDev.ABS_X, false),
      ("Left Stick Right", JsEvDev.ABS_X, true),
      ("Left Stick Up", JsEvDev.ABS_Y, false),
      ("Left Stick Down", JsEvDev.ABS_Y, true),
      ("Right Stick Left", JsEvDev.ABS_RX, false),
      ("Right Stick Right", JsEvDev.ABS_RX, true),
      ("Right Stick Up", JsEvDev.ABS_RY, false),
      ("Right Stick Down", JsEvDev.ABS_RY, true),
      ("Left Trigger", JsEvDev.ABS_Z, true),
      ("Right Trigger", JsEvDev.ABS_RZ, true),
      ("DPad Left", JsEvDev.ABS_HAT0X, false),
      ("DPad Right", JsEvDev.ABS_HAT0X, true),
      ("DPad Up", JsEvDev.ABS_HAT0Y, false),
      ("DPad Down", JsEvDev.ABS_HAT0Y, true)
    )

    val jsSelect = new ComboBox(jnames) {
      maximumSize = (Int.MaxValue, preferredSize.height)
    }

    val buttBars = Map.empty ++ (for ((bname, bcode) <- buttons) yield (bcode, new ProgressBar))
    val axesBars = Map.empty ++ (for ((aname, acode, apos) <- axes) yield ((acode, apos), new ProgressBar))

    val buttButtons = Map.empty ++ (for ((bname, bcode) <- buttons) yield (bcode, new Button(".")))
    val axesButtons = Map.empty ++ (for ((aname, acode, apos) <- axes) yield ((acode, apos), new Button(".")))

    val jst = new JsThread(jinfos(0))
    listenTo(jsSelect.selection)
    reactions += {
      case SelectionChanged(_) => jst.start(jinfos(jsSelect.selection.index))
    }

    def input(evtype: Short, code: Short, value: Int) {
      evtype match {
        case JsEvDev.EV_KEY => buttBars.get(code) foreach (_.value = value)
        case JsEvDev.EV_ABS => {
          axesBars.get(code, true) foreach (_.value = 0 max value)
          axesBars.get(code, false) foreach (_.value = -(0 min value))
        }
        case _ =>
      }
    }

    contents = new BoxPanel(Orientation.Vertical) {
      border = Swing.EmptyBorder(contGap, contGap, contGap, contGap)
      contents += jsSelect
      contents += Swing.VStrut(unrelGap)
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new BoxPanel(Orientation.Vertical) {
          for ((bname, bcode) <- buttons) {
            contents += Swing.VStrut(1)
            contents += new BoxPanel(Orientation.Horizontal) {
              val bar = buttBars(bcode)
              val butt = buttButtons(bcode)
              val h = butt.preferredSize.height-3
              maximumSize = (Int.MaxValue, h)
              bar.allSizes = (h, h)
              butt.allSizes = (4*h, h)
              bar.max = 1
              butt.reactions += {
                case ButtonClicked(_) =>
                  KeyDialog.open()
                  butt2Key.synchronized { butt2Key(bcode) = KeyDialog.key }
                  butt.text = KeyDialog.key.toString
              }
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
          for ((aname, acode, apos) <- axes) {
            contents += Swing.VStrut(1)
            contents += new BoxPanel(Orientation.Horizontal) {
              val bar = axesBars(acode, apos)
              val butt = axesButtons(acode, apos)
              val h = butt.preferredSize.height-3
              maximumSize = (Int.MaxValue, h)
              bar.allSizes = (4*h, h)
              butt.allSizes = (4*h, h)
              bar.max = if (apos) jinfos(0).axes(acode)._2 else -jinfos(0).axes(acode)._1
              butt.reactions += {
                case ButtonClicked(_) =>
                  KeyDialog.open()
                  axis2Key.synchronized { axis2Key((acode, apos)) = KeyDialog.key }
                  butt.text = KeyDialog.key.toString
              }
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
  }

  override def shutdown() {
    top.jst.stop()
    JsEvDev.closePipe()
  }
}

class JsThread(private var info: Joystick.Info) {
  @volatile
  private var cont = true
  private var f = newFuture(info.filename)
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
        Main.butt2Key.synchronized {
          for (key <- Main.butt2Key.get(evcode))
            if (evvalue == 0)
              robot.keyRelease(key.id)
            else
              robot.keyPress(key.id)
        }
      case JsEvDev.EV_ABS =>
        val oldval = axesValues.getOrElse(evcode, 0)
        axesValues(evcode) = evvalue
        Main.axis2Key.synchronized {
          for ((min, max) <- info.axes.get(evcode)) {
            for (key <- Main.axis2Key.get((evcode, false))) chkbnd(-oldval, -evvalue, -min >> 1, key)
            for (key <- Main.axis2Key.get((evcode, true))) chkbnd(oldval, evvalue, max >> 1, key)
          }
        }
      case _ =>
    }
    if (evtype > 0)
      Swing.onEDT { Main.top.input(evtype, evcode, evvalue) }
  }

  private def read(js: Joystick) {
    val buf = ByteBuffer.allocateDirect(JsEvDev.INPUT_EVENT_SIZE<<10)
    buf.order(ByteOrder.nativeOrder())
    while (cont) {
      val cnt = js.read(buf)
      if (cnt <= 0) throw new Exception
      buf.rewind()
      while (buf.position < cnt) {
        buf.position(buf.position + JsEvDev.INPUT_EVENT_TYPE_OFFSET);
        processEvent(buf.getShort(), buf.getShort(), buf.getInt())
      }
    }
  }

  private def newFuture(filename: String) = Future {
    blocking {
      Joystick.withFilename(info.filename)(read)
    }
  }

  def start(info: Joystick.Info) {
    stop()
    this.info = info
    axesValues.clear()
    f = newFuture(info.filename)
  }

  def stop() {
    cont = false
    JsEvDev.cancelOn()
    Await.ready(f, Duration.Inf)
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
  type AxesInfo = Map[Short, (Int, Int)]
  class Info(val filename: String, val axes: AxesInfo)

  def withFilename[T](filename: String)(body: Joystick => T): Option[T] = {
    val fd = JsEvDev.open(filename)
    if (fd >= 0)
      try { Some(body(new Joystick(fd))) }
      finally { JsEvDev.close(fd) }
    else
      None
  }

  def scanEvDir(): Seq[(String, Info)] = {
    (for {
      file <- Files.newDirectoryStream(Paths.get("/dev/input")).asScala
      if (file.getFileName.toString matches "event[0-9]+")
      result <- withFilename(file.toString) { js =>
        for (axesInfo <- js.checkXBox())
          yield (s"${js.getName()} (${file.getFileName})", new Info(file.toString, axesInfo))
      }.flatten.toList
    } yield result).toSeq
  }

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
      Map.empty ++ (for {
        axis <- axes
        if testBit(absBits, axis)
        info = getAbsInfo(axis)
        min = info(1)
        max = info(2)
        if (max > 0)
      } yield (axis, (min, max)))
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
