package com.tehuti.reader.reader.format

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import javax.inject.Inject

@OptIn(ExperimentalReadiumApi::class)
class EpubEngine @Inject constructor() : ReaderEngine {

    override val fragmentClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override fun createFragmentFactory(publication: Publication, initialLocator: Locator?): FragmentFactory {
        val locator = initialLocator ?: publication.locatorFromLink(publication.readingOrder.first())
        val factory = EpubNavigatorFactory(publication)
        return factory.createFragmentFactory(
            locator,
            null,
            EpubPreferences(),
            object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {}
            },
            object : EpubNavigatorFragment.PaginationListener {},
            EpubNavigatorFragment.Configuration(),
        )
    }
}
