package app.campfire.libraries.test

import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator

class FakeLibraryItemValidator : LibraryItemValidator {

  var nextValidation: LibraryItemValidation = LibraryItemValidation.Success
  override fun validate(item: LibraryItem): LibraryItemValidation {
    return nextValidation
  }
}
