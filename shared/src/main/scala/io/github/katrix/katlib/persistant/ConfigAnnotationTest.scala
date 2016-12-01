package io.github.katrix.katlib.persistant

@configuration("test.conf") trait ConfigAnnotationTest {

	@comment("a simple int")       val simple  : Int      = 5
	@comment("a sequence of ints") val sequence: Seq[Int] = Seq(1, 2, 3)

	//val `with space`: String   = "this one has a space"
	//val `plus+`: String = "Another char"

	val nested1: Nested1
	trait Nested1 {
		@comment("a nested object") val nested1Obj: String = "lvl 1"
		val nested2: Nested2
		trait Nested2 {
			val nested2Obj: String = "lvl 2"
			val nested3: Nested3
			trait Nested3 {
				val nested3Obj: String = "lvl 3"
				val nested4: Nested4
				trait Nested4 {
					val nested4Obj: String = "lvl 4"
				}
			}
		}
	}
}