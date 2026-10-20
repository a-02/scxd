import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._

import com.monovore.decline._
import com.monovore.decline.effect._
import os.*
import fansi.Color
import fansi.Str
import fansi.Attr
import cats.data.ValidatedNel

import Fuzz._

object Scxd
    extends CommandIOApp(
      name = "scxd",
      header = "toy hex editor",
      version = "0.0.0"
    ) {

  private val BytesPerRow = 16
  private val OffsetWidth = 8

  private val wd = os.pwd

  private val pathOpts: Opts[Path] =
    Opts.argument[String](metavar = "path").map(str => wd / str)

  private def readBytesFromFile(path: Path): IO[Array[Byte]] =
    IO.blocking:
      os.read.bytes(path)

  private def run(path: Path): IO[ExitCode] =
    readBytesFromFile(path)
      .flatMap(bytes => IO.println(render(bytes)))
      .as(ExitCode.Success)

  private def findReplaceSubcommand: Opts[IO[ExitCode]] =
    Opts.subcommand("find-replace", "what do you think it does.") {
      (pathOpts, findByte, replaceByte).mapN(findReplace)
    }

  private def dumpSubcommand: Opts[IO[ExitCode]] =
    Opts.subcommand("dump", "print the hex to the screen please.") {
      pathOpts.map(run)
    }

  private def fuzzSubcommand: Opts[IO[ExitCode]] =
    Opts.subcommand("fuzz", "fiddle with the bytes some.") {
      (pathOpts, intensityOpt).mapN(fuzzBytes)
    }

  private def fuzzBytes(path: Path, intensity: Double): IO[ExitCode] =
    for
      bytes <- readBytesFromFile(path)
      res = Fuzz.fuzz(bytes, intensity)
      _ <- IO.blocking {
        val temp = os.temp()
        os.write.over(temp, res)
        os.move.over(temp, path)
      }
      _ <- IO.println(s"fuzzed ${bytes.length} bytes with intensity ${intensity}")
    yield ExitCode.Success

  private def findReplace(path: Path, find: Byte, replace: Byte): IO[ExitCode] =
    for
      bytes <- readBytesFromFile(path)
      res = bytes.map(byte => if byte == find then replace else byte)
      _ <- IO.blocking {
        val temp = os.temp()
        os.write.over(temp, res)
        os.move.over(temp, path)
      }
      _ <- IO.println(s"overwrote all ${asHex(find)} with ${asHex(replace)}.")
    yield ExitCode.Success

  private def parseByte(str: String): ValidatedNel[String, Byte] =
    if str.length > 2
    then "How can a byte be more than 2 hex digits?".invalidNel
    else Integer.parseInt(str, 16).toByte.validNel

  private val findByte: Opts[Byte] =
    Opts.argument[String](metavar = "find").mapValidated(parseByte)

  private val replaceByte: Opts[Byte] =
    Opts.argument[String](metavar = "find").mapValidated(parseByte)

  private val intensityOpt: Opts[Double] =
    Opts.argument[Double](metavar = "intesnity")

  private def offset(i: Int): String =
    s"%0${OffsetWidth}x".format(i)

  private def hexByte(b: Byte): String =
    colorize(b)(asHex(b)).render

  private def asHex(i: Int): String =
    "%02X".format(i & 0xff)

  private def asHex(b: Byte): String =
    "%02X".format(b)

  private def colorize(b: Byte): Attr =
    (b & 0xff) match
      case 0x00 => Color.DarkGray
      case 0xff => Color.LightGray
      // case 0x20 => Color.Red
      case _ => Color.Cyan

  private def columnHeader: String =
    val padding = " " * (OffsetWidth + 2)
    padding + (0 until BytesPerRow).map(i => asHex(i & 0xff)).mkString(" ")

  private def render(bytes: Array[Byte]): String =
    val rows = bytes
      .grouped(BytesPerRow)
      .zipWithIndex
      .map((row, idx) =>
        s"${offset(idx * BytesPerRow)}: ${row.map(hexByte).mkString(" ")}"
      )
    (columnHeader +: rows.toList).mkString("\n")

  override def main: Opts[IO[ExitCode]] =
    dumpSubcommand
      .orElse(findReplaceSubcommand)
      .orElse(fuzzSubcommand)
      .map(io =>
        io.handleErrorWith((e: Throwable) =>
          Console[IO].errorln(s"scxd: ${e.getMessage}").as(ExitCode.Error)
        )
      )
}
