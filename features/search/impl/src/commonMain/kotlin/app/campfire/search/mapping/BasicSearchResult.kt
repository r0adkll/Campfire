package app.campfire.search.mapping

import app.campfire.core.model.BasicSearchResult
import app.campfire.network.models.NarratorSearchResult
import app.campfire.network.models.TagSearchResult

fun NarratorSearchResult.asDomainModel(): BasicSearchResult = BasicSearchResult(name, numBooks)
fun TagSearchResult.asDomainModel(): BasicSearchResult = BasicSearchResult(name, numItems)
