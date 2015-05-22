package org

/**
 * Global definitions
 */
package object strucs {

  // Thanks to Regis Jean-Gilles in http://stackoverflow.com/questions/6909053/enforce-type-difference
  @annotation.implicitNotFound(msg = "Cannot prove that ${A} <:!< ${B}.")
  trait <:!<[A,B]
  object <:!< {
    class Impl[A, B]
    object Impl {
      implicit def nsub[A, B] : A Impl B = null
      implicit def nsubAmbig1[A, B>:A] : A Impl B = null
      implicit def nsubAmbig2[A, B>:A] : A Impl B = null
    }

    implicit def foo[A,B]( implicit e: A Impl B ): A <:!< B = null
  }
}
