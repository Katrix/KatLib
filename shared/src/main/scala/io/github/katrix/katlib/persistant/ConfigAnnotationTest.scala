package io.github.katrix.katlib.persistant

@configTrait("test")
trait ConfigAnnotationTest {

	val simple      : Int      = 5
	val seqence     : Seq[Int] = Seq(1, 2, 3)
	val `with space`: String   = "this one has a space"
	val `plus+`: String = "Another char"

	object Nested1 {
		val nested1: String = "lvl 1"
		object Nested2 {
			val nested2: String = "lvl 2"
			object Nested3 {
				val nested3: String = "lvl 3"
			}
		}
	}
}