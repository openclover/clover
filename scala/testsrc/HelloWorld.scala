package foo.bar

class Foo {
  def someMultiLineMethod(arg: String) = {
    println(arg)
    println(arg)

    val nums = List(1, 2, 3, 4, 5, 6, 7, 8)
    println(CurryTest.filter(nums, CurryTest.modN(2)))
    println(CurryTest.filter(nums, CurryTest.modN(3)))
  }

  def someLiteral: String = ""

}

object HelloWorld {
  def main(args: Array[String]) = new Foo().someMultiLineMethod("Hello, world")
}

object CurryTest {

  def filter(xs: List[Int], p: Int => Boolean): List[Int] =
    if (xs.isEmpty) xs
    else if (p(xs.head)) xs.head :: filter(xs.tail, p)
    else filter(xs.tail, p)

  def modN(n: Int)(x: Int) = ((x % n) == 0)
}