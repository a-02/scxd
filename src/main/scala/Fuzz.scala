import scala.util.Random

object Fuzz {

  val fuzzy = 3

  val prng = new Random()
  
  def fuzz(inByteArray: Array[Byte], intensity: Double): Array[Byte] =
    inByteArray.map(b =>
      if prng.nextDouble() < intensity then prng.nextInt(256).toByte else b
    )
}
