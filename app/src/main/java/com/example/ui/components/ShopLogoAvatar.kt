package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.Gold500
import java.io.File

@Composable
fun ShopLogoAvatar(
    logoUri: String?,
    size: Dp = 64.dp,
    shape: Shape = CircleShape,
    borderColor: Color = Gold500,
    borderWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier
) {
    val customBitmap = remember(logoUri) {
        if (!logoUri.isNullOrBlank()) {
            try {
                val f = File(logoUri)
                if (f.exists()) {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inScaled = false
                        inDither = true
                    }
                    BitmapFactory.decodeFile(f.absolutePath, opts)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = "Custom Brand Logo",
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(borderWidth, borderColor, shape)
                .testTag("custom_shop_logo_avatar")
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.sentry_store_logo_1787989266987),
            contentDescription = "SENTRY STORE Brand Logo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(borderWidth, borderColor, shape)
                .testTag("default_shop_logo_avatar")
        )
    }
}


