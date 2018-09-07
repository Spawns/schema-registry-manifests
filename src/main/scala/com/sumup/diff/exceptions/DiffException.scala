package com.sumup.diff.exceptions

trait DiffException {
  // NOTE: Require implementers of the trait
  // to also extend `Throwable`.
  self: Throwable =>
}
