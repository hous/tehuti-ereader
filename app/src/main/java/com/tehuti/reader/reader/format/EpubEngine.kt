package com.tehuti.reader.reader.format

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.tehuti.reader.domain.model.LookupType
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import javax.inject.Inject

private const val MENU_ID_DICTIONARY = 1001
private const val MENU_ID_WIKIPEDIA = 1002

@OptIn(ExperimentalReadiumApi::class)
class EpubEngine @Inject constructor() : ReaderEngine {

    override val fragmentClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override fun createFragmentFactory(
        publication: Publication,
        initialLocator: Locator?,
        onSelectionAction: (LookupType) -> Unit,
    ): FragmentFactory {
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
            EpubNavigatorFragment.Configuration(
                selectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.add(0, MENU_ID_DICTIONARY, 0, "Dictionary")
                        menu.add(0, MENU_ID_WIKIPEDIA, 0, "Wikipedia")
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val type = when (item.itemId) {
                            MENU_ID_DICTIONARY -> LookupType.DICTIONARY
                            MENU_ID_WIKIPEDIA -> LookupType.WIKIPEDIA
                            else -> return false
                        }
                        onSelectionAction(type)
                        mode.finish()
                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {}
                },
            ),
        )
    }
}
