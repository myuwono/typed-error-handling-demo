package io.github.myuwono.petshop

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Level1Test : FunSpec() {
  init {
    test("should be ok") {
      "foo" shouldBe "foo"
    }
  }
}
