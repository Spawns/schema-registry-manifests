package com.sumup.testutils.builders

trait Builder[T] {
  def build(): T
}
