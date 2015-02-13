/**
 * http://stackoverflow.com/a/6602501/649835
 */
package utils
object Let {
    def let[A,B](a:A)(f:A=>B):B = f(a)
}
