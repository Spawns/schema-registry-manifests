package com.sumup.storage.repositories.exceptions

 trait RepositoryException {
   // NOTE: Require implementers of the trait
   // to also extend `Throwable`.
   self: Throwable =>
}
