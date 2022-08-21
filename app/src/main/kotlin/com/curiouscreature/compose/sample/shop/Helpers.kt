/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.curiouscreature.compose.sample.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.curiouscreature.compose.R

val ProductColors = mapOf(
    "Fiery Red" to Color(0.634f, 0.000f, 0.000f),
    "Deep Blue" to Color(0.000f, 0.480f, 0.596f),
    "Swirly Orange" to Color(0.874f, 0.522f, 0.080f),
    "Fantastic Yellow" to Color(0.859f, 0.808f, 0.020f)
)

val ProductColorProgression = mapOf(
    "Fiery Red" to "Deep Blue",
    "Deep Blue" to "Swirly Orange",
    "Swirly Orange" to "Fantastic Yellow",
    "Fantastic Yellow" to "Fiery Red"
)

val ProductColorSampleImages = mapOf(
    "Fiery Red" to R.drawable.sample_red,
    "Deep Blue" to R.drawable.sample_blue,
    "Swirly Orange" to R.drawable.sample_orange,
    "Fantastic Yellow" to R.drawable.sample_yellow
)

fun nextProductColor(color: String) = ProductColorProgression.getOrElse(color) { color }

fun productColor(product: Product) =
    ProductColors.getOrElse(product.color) { Color(1.0f, 1.0f, 1.0f) }

val String.isProductColor: Boolean
    get() = this != "N/A"

@Composable
fun SampleImage(color: String) {
    Image(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        painter = painterResource(id = ProductColorSampleImages.getOrElse(color) { R.drawable.sample_orange }),
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
}
