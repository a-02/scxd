import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._

import com.monovore.decline._
import com.monovore.decline.effect._
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import fansi.Color
import fansi.Str
import fansi.Attr

object Scxd
    extends CommandIOApp(
      name = "scxd",
      header = "toy hex editor",
      version = "0.0.0"
    ) {

  private val BytesPerRow = 16
  private val OffsetWidth = 8

  private val pathOpts: Opts[Path] =
    Opts.argument[Path](metavar = "path")

  private def readBytesFromFile(path: Path): IO[Array[Byte]] =
    IO.blocking:
      Files.readAllBytes(path)

  private def run(path: Path): IO[ExitCode] =
    readBytesFromFile(path)
      .flatMap(bytes => IO.println(render(bytes)))
      .as(ExitCode.Success)
      .handleErrorWith((e: Throwable) =>
        Console[IO].errorln(s"scxd: ${e.getMessage}").as(ExitCode.Error)
      )

  private def offset(i: Int): String =
    s"%0${OffsetWidth}x".format(i)

  private def hexByte(b: Byte): String =
    colorize(b)("%02X".format(b & 0xff)).render

  private def colorize(b: Byte): Attr =
    (b & 0xff) match
      case 0x00 => Color.DarkGray
      case 0xff => Color.LightGray
      // case 0x20 => Color.Red
      case _    => Color.Cyan

  private def columnHeader: String =
    val padding = " " * (OffsetWidth + 2)
    padding + (0 until BytesPerRow).map("%02X".format(_)).mkString(" ")

  private def render(bytes: Array[Byte]): String =
    val rows = bytes
      .grouped(BytesPerRow)
      .zipWithIndex
      .map((row, idx) =>
        s"${offset(idx * BytesPerRow)}: ${row.map(hexByte).mkString(" ")}"
      )
    (columnHeader +: rows.toList).mkString("\n")

  override def main: Opts[IO[ExitCode]] =
    pathOpts.map(run)
}
