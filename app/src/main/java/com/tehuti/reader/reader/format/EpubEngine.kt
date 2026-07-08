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
private const val MENU_ID_EXPLAIN = 1003

@OptIn(ExperimentalReadiumApi::class)
class EpubEngine @Inject constructor() : ReaderEngine {

    override val fragmentClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override fun createFragmentFactory(
        publication: Publication,
        initialLocator: Locator?,
        onSelectionAction: (LookupType) -> Unit,
        isAiExplainAvailable: () -> Boolean,
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
                        // Only offered when on-device AI is actually usable on this device —
                        // absent entirely rather than present-then-erroring.
                        if (isAiExplainAvailable()) {
                            menu.add(0, MENU_ID_EXPLAIN, 0, "[AI] Explain the context")
                        }
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val type = when (item.itemId) {
                            MENU_ID_DICTIONARY -> LookupType.DICTIONARY
                            MENU_ID_WIKIPEDIA -> LookupType.WIKIPEDIA
                            MENU_ID_EXPLAIN -> LookupType.AI_EXPLAIN
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
