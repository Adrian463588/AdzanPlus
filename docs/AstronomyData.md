# Astronomy data provenance

The Android Star Map assets are derived from public astronomical catalog data, not generated sample values:

- `app/src/main/assets/star_catalog.json` is a deterministic 500-star subset of the Hipparcos-based bright-star catalog from [johanley/star-catalog](https://github.com/johanley/star-catalog). The source repository and its generated catalog are released under CC0-1.0.
- `app/src/main/assets/constellation_lines.json` contains 40 constellation line sets converted from the HIP output of [johanley/constellation-lines](https://github.com/johanley/constellation-lines), released under CC0-1.0.

The build asset transformation keeps the real HIP identifier, J2000 right ascension/declination, visual magnitude, and proper-name fields where available. Constellation segments are retained only when both HIP endpoints exist in the bundled 500-star subset; no coordinates or line endpoints are invented.
