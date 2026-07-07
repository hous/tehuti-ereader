package com.tehuti.reader.reader.format

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.tehuti.reader.domain.model.LookupType
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

interface ReaderEngine {
    val fragmentClass: Class<out Fragment>

    fun createFragmentFactory(
        publication: Publication,
        initialLocator: Locator?,
        onSelectionAction: (LookupType) -> Unit,
    ): FragmentFactory
}
