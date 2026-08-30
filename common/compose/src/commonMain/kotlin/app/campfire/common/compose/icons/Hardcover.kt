// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CampfireIcons.Hardcover: ImageVector
  get() {
    if (_Hardcover != null) {
      return _Hardcover!!
    }
    _Hardcover = ImageVector.Builder(
      name = "Hardcover",
      defaultWidth = 40.dp,
      defaultHeight = 40.dp,
      viewportWidth = 40f,
      viewportHeight = 40f,
    ).apply {
      path(fill = SolidColor(Color(0xFF4F46E5))) {
        moveTo(12.889f, 32.598f)
        curveTo(12.666f, 31.766f, 13.16f, 30.911f, 13.992f, 30.688f)
        lineTo(30.297f, 26.319f)
        curveTo(31.129f, 26.096f, 31.985f, 26.59f, 32.208f, 27.422f)
        lineTo(32.874f, 29.909f)
        curveTo(33.171f, 31.018f, 32.513f, 32.159f, 31.403f, 32.456f)
        lineTo(18.111f, 36.018f)
        curveTo(15.892f, 36.612f, 13.612f, 35.295f, 13.017f, 33.076f)
        lineTo(12.889f, 32.598f)
        close()
      }
      path(fill = SolidColor(Color(0xFF4F46E5))) {
        moveTo(7.623f, 12.946f)
        curveTo(7.051f, 10.812f, 8.318f, 8.619f, 10.452f, 8.047f)
        lineTo(16.885f, 32.057f)
        lineTo(13.021f, 33.092f)
        lineTo(7.623f, 12.946f)
        close()
      }
      path(fill = SolidColor(Color(0xFF4338CA))) {
        moveTo(29.336f, 24.432f)
        lineTo(31.268f, 23.914f)
        lineTo(32.358f, 27.985f)
        curveTo(32.644f, 29.052f, 32.011f, 30.149f, 30.944f, 30.434f)
        lineTo(29.336f, 24.432f)
        close()
      }
      path(fill = SolidColor(Color(0xFF6366F1))) {
        moveTo(26.445f, 5.915f)
        curveTo(26.147f, 4.805f, 25.007f, 4.147f, 23.897f, 4.444f)
        lineTo(10.529f, 8.026f)
        curveTo(9.419f, 8.324f, 8.761f, 9.464f, 9.058f, 10.573f)
        lineTo(14.953f, 32.575f)
        lineTo(22.646f, 30.514f)
        curveTo(23.199f, 30.365f, 23.527f, 29.798f, 23.378f, 29.245f)
        curveTo(23.23f, 28.692f, 23.558f, 28.125f, 24.111f, 27.976f)
        lineTo(29.795f, 26.454f)
        curveTo(30.904f, 26.156f, 31.563f, 25.016f, 31.265f, 23.906f)
        lineTo(26.445f, 5.915f)
        close()
      }
      path(fill = SolidColor(Color(0xFF312E81))) {
        moveTo(21.095f, 11.281f)
        curveTo(21.145f, 10.665f, 21.941f, 10.451f, 22.293f, 10.96f)
        lineTo(22.442f, 11.176f)
        curveTo(22.551f, 11.334f, 22.724f, 11.436f, 22.915f, 11.457f)
        lineTo(23.237f, 11.49f)
        curveTo(23.838f, 11.553f, 24.045f, 12.323f, 23.556f, 12.678f)
        lineTo(23.294f, 12.868f)
        curveTo(23.138f, 12.981f, 23.039f, 13.156f, 23.024f, 13.348f)
        lineTo(23.003f, 13.61f)
        curveTo(22.952f, 14.226f, 22.156f, 14.439f, 21.805f, 13.931f)
        lineTo(21.655f, 13.715f)
        curveTo(21.546f, 13.557f, 21.373f, 13.454f, 21.182f, 13.434f)
        lineTo(20.86f, 13.401f)
        curveTo(20.259f, 13.338f, 20.053f, 12.567f, 20.542f, 12.213f)
        lineTo(20.804f, 12.022f)
        curveTo(20.959f, 11.909f, 21.058f, 11.734f, 21.073f, 11.543f)
        lineTo(21.095f, 11.281f)
        close()
      }
      path(fill = SolidColor(Color(0xFF312E81))) {
        moveTo(18.303f, 16.318f)
        curveTo(18.353f, 15.701f, 19.149f, 15.488f, 19.501f, 15.997f)
        lineTo(20.563f, 17.534f)
        curveTo(20.673f, 17.692f, 20.846f, 17.794f, 21.037f, 17.814f)
        lineTo(22.914f, 18.01f)
        curveTo(23.514f, 18.073f, 23.721f, 18.844f, 23.232f, 19.198f)
        lineTo(21.705f, 20.307f)
        curveTo(21.549f, 20.42f, 21.451f, 20.595f, 21.435f, 20.786f)
        lineTo(21.283f, 22.648f)
        curveTo(21.233f, 23.265f, 20.437f, 23.478f, 20.085f, 22.969f)
        lineTo(19.023f, 21.433f)
        curveTo(18.914f, 21.275f, 18.741f, 21.172f, 18.55f, 21.152f)
        lineTo(16.672f, 20.956f)
        curveTo(16.072f, 20.893f, 15.865f, 20.123f, 16.354f, 19.768f)
        lineTo(17.882f, 18.659f)
        curveTo(18.037f, 18.547f, 18.136f, 18.372f, 18.151f, 18.18f)
        lineTo(18.303f, 16.318f)
        close()
      }
      path(fill = SolidColor(Color(0xFFEEF2FF))) {
        moveTo(14.953f, 32.575f)
        curveTo(14.657f, 31.47f, 15.313f, 30.334f, 16.418f, 30.038f)
        lineTo(29.872f, 26.433f)
        lineTo(30.944f, 30.434f)
        lineTo(17.49f, 34.04f)
        curveTo(16.385f, 34.336f, 15.249f, 33.68f, 14.953f, 32.575f)
        close()
      }
    }.build()

    return _Hardcover!!
  }

@Suppress("ObjectPropertyName")
private var _Hardcover: ImageVector? = null
