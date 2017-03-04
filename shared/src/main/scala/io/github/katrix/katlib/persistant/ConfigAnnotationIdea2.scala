package io.github.katrix.katlib.persistant

@cfg("test.conf")
class ConfigAnnotationIdea2 {

  @comment("a simple int") val simple:         Int      = 5
  @comment("a sequence of ints") val sequence: Seq[Int] = Seq(1, 2, 3)

  //val `with space`: String   = "this one has a space"
  //val `with-minus`: String   = "this one has a space"
  //val `plus+`: String = "Another char"

  val nested1: Nested1 = new Nested1
  class Nested1 {
    @comment("a nested object") val nested1Obj: String = "lvl 1"
    val nested2: Nested2 = new Nested2
    class Nested2 {
      val nested2Obj: String = "lvl 2"
      val nested3: Nested3 = new Nested3
      class Nested3 {
        val nested3Obj: String = "lvl 3"
        val nested4: Nested4 = new Nested4
        class Nested4 {
          val nested4Obj: String = "lvl 4"
        }
      }
    }
  }
}