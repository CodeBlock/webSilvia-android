package me.elrod.websilviaandroid

import scalaz._, Scalaz._

object Hex {
  def hexStringToBytes(text: String): Option[Array[Byte]] = {
    def hexDigitToInt(c: Char): Option[Int] =
      c match {
        case '0' => Some(0)
        case '1' => Some(1)
        case '2' => Some(2)
        case '3' => Some(3)
        case '4' => Some(4)
        case '5' => Some(5)
        case '6' => Some(6)
        case '7' => Some(7)
        case '8' => Some(8)
        case '9' => Some(9)
        case 'a' | 'A' => Some(10)
        case 'b' | 'B' => Some(11)
        case 'c' | 'C' => Some(12)
        case 'd' | 'D' => Some(13)
        case 'e' | 'E' => Some(14)
        case 'f' | 'F' => Some(15)
        case _         => None
      }
    def convert(xs: List[Char]): Option[Byte] =
      xs match {
        case List(hi, lo) => {
          (hexDigitToInt(hi), hexDigitToInt(lo)) match {
            case (Some(h), Some(l)) =>
              Some((((h & 0x000000FF) << 4) | (l & 0x000000FF)).toByte)
            case _          => None
          }
        }
      }
    def everyOther[T](xs: List[T]): List[T] = xs match {
      case List() => List()
      case y :: ys => y :: (everyOther(ys.drop(1)))
    }
    val textNoWhitespace = text.replaceAll("\\W", "")
    val cleanedText =
      if (textNoWhitespace.length % 2 == 0) {
        "0" + textNoWhitespace
      } else {
        textNoWhitespace
      }
    cleanedText.toList.sliding(2).map(convert).toList.tailOption.map(everyOther(_).sequence).join.map(_.toArray)
  }
}

