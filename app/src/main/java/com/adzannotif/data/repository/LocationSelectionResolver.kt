package com.adzannotif.data.repository

import com.adzannotif.domain.model.LocationInfo

/** Resolves the selected location without inventing a coordinate fallback. */
internal object LocationSelectionResolver {
    fun resolve(selected: LocationInfo?, saved: List<LocationInfo>): LocationInfo? =
        selected ?: saved.firstOrNull()
}
