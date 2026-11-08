package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsArticle
import com.example.data.model.WeatherForecast
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.SpaceGroteskFontFamily

private val AVAILABLE_CITIES = listOf(
    "San Francisco",
    "New York",
    "Tokyo",
    "London",
    "Seoul",
    "Singapore"
)

/**
 * Pixel Launcher left-pane style Discover & Live Atmospheric Weather Page.
 */
@Composable
fun DiscoverFeedPage(
    weather: WeatherForecast?,
    newsArticles: List<NewsArticle>,
    selectedCity: String,
    onSelectCity: (String) -> Unit,
    onRefreshFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("discover_feed_page"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.DISCOVER_NEWS,
                        size = 28.dp,
                        tint = tokens.accentPrimary,
                        secondaryTint = tokens.accentSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pixel Discover & Sky",
                            style = MaterialTheme.typography.headlineMedium,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Personalized Briefings • Open-Meteo Live",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshFeed,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tokens.cardFillTop)
                        .testTag("refresh_discover_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh weather and news feed",
                        tint = tokens.accentPrimary
                    )
                }
            }
        }

        // City Selector Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVAILABLE_CITIES.forEach { city ->
                    val isSelected = city.equals(selectedCity, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) tokens.accentPrimary else tokens.cardFillTop
                            )
                            .clickable { onSelectCity(city) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("city_chip_${city.lowercase().replace(' ', '_')}")
                    ) {
                        Text(
                            text = city,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) androidx.compose.ui.graphics.Color(0xFF041326) else tokens.textPrimary
                        )
                    }
                }
            }
        }

        // Detailed Weather Forecast Glass Card
        item {
            val forecast = weather ?: WeatherForecast(
                city = selectedCity,
                temperatureCelsius = 21,
                highCelsius = 24,
                lowCelsius = 15,
                conditionText = "Clear Prismatic Sky",
                windKmh = 12,
                humidityPercent = 52,
                aqiLabel = "AQI 22 • Pristine",
                hourly = emptyList()
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.WEATHER_SUN_CLOUD,
                                    size = 22.dp,
                                    tint = tokens.accentPrimary,
                                    secondaryTint = tokens.accentSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = forecast.city.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tokens.accentPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${forecast.temperatureCelsius}°",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 54.sp,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = forecast.conditionText,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(tokens.accentSecondary.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = forecast.aqiLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "H: ${forecast.highCelsius}°  L: ${forecast.lowCelsius}°",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wind ${forecast.windKmh} km/h • Humidity ${forecast.humidityPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary
                            )
                        }
                    }

                    if (forecast.hourly.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            forecast.hourly.forEach { slot ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(tokens.cardFillBottom)
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = slot.hourLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    CustomGlassIcon(
                                        glyph = AeroIconGlyph.WEATHER_SUN_CLOUD,
                                        size = 18.dp,
                                        tint = tokens.accentPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${slot.tempCelsius}°",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PERSONALIZED PIXEL PULSE",
                style = MaterialTheme.typography.labelLarge,
                color = tokens.accentPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(newsArticles, key = { it.id }) { article ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("news_card_${article.id}"),
                cornerRadius = 24.dp,
                onClick = {
                    if (article.url.startsWith("http")) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(tokens.accentPrimary.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = article.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.accentPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${article.source} • ${article.timeAgo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }

                        if (article.url.startsWith("http")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open article",
                                tint = tokens.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${article.readMinutes} min distraction-free read",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentSecondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
