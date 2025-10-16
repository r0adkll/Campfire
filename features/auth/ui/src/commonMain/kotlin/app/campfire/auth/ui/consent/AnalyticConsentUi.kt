package app.campfire.auth.ui.consent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.auth.api.screen.AnalyticConsentScreen
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.auth.ui.composables.SinglePaneLayout
import app.campfire.auth.ui.composables.TwoPaneLayout
import app.campfire.auth.ui.login.composables.TitleBanner
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AreaChart
import app.campfire.common.compose.icons.rounded.AreaChartFilled
import app.campfire.common.compose.icons.rounded.Crash
import app.campfire.common.compose.icons.rounded.CrashFilled
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(AnalyticConsentScreen::class, UserScope::class)
@Composable
fun AnalyticConsent(
  state: AnalyticConsentUiState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding(),
  ) {
    val windowSizeClass = LocalWindowSizeClass.current
    if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Large) {
      TwoPaneLayout {
        AnalyticConsentContent(
          state = state,
          modifier = Modifier,
        )
      }
    } else {
      SinglePaneLayout(
        logoTitle = {
          TitleBanner(
            Modifier.padding(
              horizontal = 24.dp,
              vertical = 48.dp,
            ),
          )
        },
      ) {
        AnalyticConsentContent(
          state = state,
          modifier = Modifier
            .fillMaxHeight()
            .navigationBarsPadding(),
        )
      }
    }
  }
}

@Composable
private fun AnalyticConsentContent(
  state: AnalyticConsentUiState,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    Column(
      modifier = Modifier
        .widthIn(max = MaxContentWidth)
        .padding(
          horizontal = 24.dp,
          vertical = 16.dp,
        )
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CrashReportingConsentItem(
        crashReportingEnabled = state.crashReportingEnabled,
        onCheckedChange = {
          state.eventSink(AnalyticConsentUiEvent.CrashReporting(it))
        },
      )

      AnalyticReportingConsentItem(
        analyticReportingEnabled = state.analyticReportingEnabled,
        onCheckedChange = {
          state.eventSink(AnalyticConsentUiEvent.AnalyticReporting(it))
        },
      )

      Spacer(Modifier.height(88.dp))
    }

    ExtendedFloatingActionButton(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp),
      icon = {
        Icon(Icons.Rounded.DoneOutline, contentDescription = null)
      },
      text = {
        Text("Finish")
      },
      onClick = {
        state.eventSink(AnalyticConsentUiEvent.ApplyConsent)
      },
    )
  }
}

@Composable
private fun CrashReportingConsentItem(
  crashReportingEnabled: Boolean = false,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  ConsentItem(
    modifier = modifier,
    enabled = crashReportingEnabled,
    onCheckedChange = onCheckedChange,
    leadingIcon = {
      Icon(
        if (crashReportingEnabled) CampfireIcons.Rounded.CrashFilled else CampfireIcons.Rounded.Crash,
        contentDescription = null,
      )
    },
    title = {
      Text("Send developer analytics")
    },
    description = { isCollapsed ->
      Text(
        buildAnnotatedString {
          append(
            "Enable Campfire to collect and send crash reports as well as other developer related metrics to help " +
              "improve the stability and performance of this application",
          )
          if (!isCollapsed) {
            appendLine().appendLine()
            withStyle(
              SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
              ),
            ) {
              appendLine("What we collect")
            }
            appendBulletedList(
              "Anonymized crash reports and logs",
              "Performance metrics and spans",
            )
            appendLine()
            withStyle(
              SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
              ),
            ) {
              appendLine("What we don't collect")
            }
            appendBulletedList(
              "Any data about your servers, or the content within such as individual library items, authors, etc",
            )
          }
        },
      )
    },
  )
}

@Composable
private fun AnalyticReportingConsentItem(
  analyticReportingEnabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  ConsentItem(
    modifier = modifier,
    enabled = analyticReportingEnabled,
    onCheckedChange = onCheckedChange,
    leadingIcon = {
      Icon(
        if (analyticReportingEnabled) CampfireIcons.Rounded.AreaChartFilled else CampfireIcons.Rounded.AreaChart,
        contentDescription = null,
      )
    },
    title = {
      Text("Send usage analytics")
    },
    description = { isCollapsed ->
      Text(
        buildAnnotatedString {
          append(
            "Enable Campfire to collect usage analytics about how you use the application. This helps us to better " +
              "understand how the application is used to inform decisions to improve the experience.",
          )
          if (!isCollapsed) {
            appendLine().appendLine()
            withStyle(
              SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
              ),
            ) {
              appendLine("What we collect")
            }
            appendBulletedList(
              "Anonymized usage metrics such as screen views, actions, and other events",
            )
            appendLine()
            withStyle(
              SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
              ),
            ) {
              appendLine("What we don't collect")
            }
            appendBulletedList(
              "Any data about your servers, the content they serve, and any personal identifiable information.",
            )
          }
        },
      )
    },
  )
}

fun AnnotatedString.Builder.appendBulletedList(vararg items: String) {
  val bulletParagraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 16.sp))
  items.forEach {
    withStyle(bulletParagraphStyle) {
      append("\u2022")
      append("\t\t")
      append(it)
    }
  }
}

@Composable
private fun ConsentItem(
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  leadingIcon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  description: @Composable (collapsed: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = if (enabled) {
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    } else {
      CardDefaults.outlinedCardColors()
    },
    border = if (enabled) null else CardDefaults.outlinedCardBorder(),
    elevation = CardDefaults.elevatedCardElevation(),
  ) {
    Column {
      Row(
        modifier = Modifier
          .clickable { onCheckedChange(!enabled) }
          .padding(
            horizontal = 24.dp,
            vertical = 16.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        leadingIcon()

        Spacer(Modifier.width(16.dp))

        Box(
          modifier = Modifier.weight(1f),
        ) {
          ProvideTextStyle(
            MaterialTheme.typography.titleMedium,
          ) {
            title()
          }
        }

        Switch(
          checked = enabled,
          onCheckedChange = onCheckedChange,
        )
      }

      var collapsed by remember { mutableStateOf(true) }

      ProvideTextStyle(
        MaterialTheme.typography.bodyMedium,
      ) {
        Box(
          modifier = Modifier
            .animateContentSize()
            .clickable {
              collapsed = !collapsed
            }
            .padding(
              start = 24.dp,
              end = 24.dp,
            ),
        ) {
          description(collapsed)
        }
      }

      TextButton(
        onClick = { collapsed = !collapsed },
        modifier = Modifier.padding(
          start = 12.dp,
          bottom = 12.dp,
        ),
      ) {
        Text("${if (collapsed) "Show" else "Hide"} details")
      }
    }
  }
}
